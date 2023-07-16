package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSIShortExitStrategy5 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy5(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (!isTrailing) {
            trailer.setAbsoluteMaxPrice(currentPrice);
            isTrailing = true;
            log.info("{} MACDOverRSIShortExitStrategy5 change trailing true", realTimeData.getSymbol());
        } else {
            trailer.updateTrailer(currentPrice);
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSIShortExitStrategy5 change trailing true, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(CLOSE_SHORT_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        }
        return null;
    }
}

