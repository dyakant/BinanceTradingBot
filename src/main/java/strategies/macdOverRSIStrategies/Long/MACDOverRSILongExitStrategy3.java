package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 */
@Slf4j
public class MACDOverRSILongExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateTrailer(currentPrice);
            if (stayInTrackAndThreePositiveHistograms(realTimeData)) {
                log.info("{} MACDOverRSILongExitStrategy3 change trailing false, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy3 executed, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(SELL_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        } else {
            if (changedDirectionAndPositiveThreeHistogram(realTimeData)) {
                trailer.setAbsoluteMaxPrice(currentPrice);
                log.info("{} MACDOverRSILongExitStrategy3 change trailing true, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = true;
            }
        }
        return null;
    }
}
