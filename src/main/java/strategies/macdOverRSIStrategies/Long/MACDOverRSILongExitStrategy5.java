package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSILongExitStrategy5 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy5(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (!isTrailing) {
            trailer.setAbsoluteMaxPrice(currentPrice);
            isTrailing = true;
            log.info("{} MACDOverRSILongExitStrategy5 change trailing true", realTimeData.getSymbol());

        } else {
            trailer.updateTrailer(currentPrice);
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy3 change trailing true, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(SELL_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        }
        return null;
    }

}
