package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.SELL_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

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
        } else {
            trailer.updateTrailer(currentPrice);
            if (trailer.needToSell(currentPrice)) {
                return new SellingInstructions(SELL_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
            }
        }
        return null;
    }

}
