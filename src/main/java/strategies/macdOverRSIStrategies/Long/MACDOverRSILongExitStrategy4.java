package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSILongExitStrategy4 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateTrailer(currentPrice);
            if (changedDirectionAndNegativeThreeHistogram(realTimeData)) {
                log.info("{} MACDOverRSILongExitStrategy4 change trailing false, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy4 executed, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(SELL_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        } else {
            if (stayInTrackAndThreeNegativeHistograms(realTimeData)) {
                log.info("{} MACDOverRSILongExitStrategy4 change trailing true, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                trailer.setAbsoluteMaxPrice(currentPrice);
                isTrailing = true;
            }
        }
        return null;
    }
}
