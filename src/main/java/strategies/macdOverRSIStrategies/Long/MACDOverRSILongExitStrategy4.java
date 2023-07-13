package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

public class MACDOverRSILongExitStrategy4 extends MACDOverRSIBaseExitStrategy {

    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        if (isTrailing) {
            double currentPrice = realTimeData.getCurrentPrice();
            trailer.updateTrailer(currentPrice);
            if (changedDirectionAndNegativeThreeHistogram(realTimeData)) {
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                return new SellingInstructions(SELL_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
            }
        } else {
            if (stayInTrackAndThreeNegativeHistograms(realTimeData)) {
                trailer.setAbsoluteMaxPrice(realTimeData.getCurrentPrice());
                isTrailing = true;
            }

        }
        return null;
    }
}
