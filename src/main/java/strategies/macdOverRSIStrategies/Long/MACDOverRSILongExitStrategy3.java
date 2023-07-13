package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 *
 */
public class MACDOverRSILongExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        if (isTrailing) {
            double currentPrice = realTimeData.getCurrentPrice();
            trailer.updateTrailer(currentPrice);
            if (stayInTrackAndThreePositiveHistograms(realTimeData)) {
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                return new SellingInstructions(SELL_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
            }
        } else {
            if (changedDirectionAndPositiveThreeHistogram(realTimeData)) {
                trailer.setAbsoluteMaxPrice(realTimeData.getCurrentPrice());
                isTrailing = true;
            }
        }
        return null;
    }
}
