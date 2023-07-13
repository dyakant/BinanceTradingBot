package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

public class MACDOverRSIShortExitStrategy1 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean currentPriceAboveSMA = realTimeData.getSMAValueAtIndex(realTimeData.getLastCloseIndex()) < realTimeData.getCurrentPrice();
        if (currentPriceAboveSMA) {
            return new SellingInstructions(CLOSE_SHORT_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
