package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

public class MACDOverRSIShortExitStrategy4 extends MACDOverRSIBaseExitStrategy {

    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        if (isTrailing) {
            double currentPrice = realTimeData.getCurrentPrice();
            trailer.updateTrailer(currentPrice);
            if (changedDirectionAndPositiveThreeHistogram(realTimeData)) {
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
            }
        } else {
            if (stayInTrackAndThreePositiveHistograms(realTimeData)) {
                isTrailing = true;
                trailer.setAbsoluteMaxPrice(realTimeData.getCurrentPrice());
            }
        }
        return null;
    }
}
