package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static positions.PositionHandler.ClosePositionTypes.SELL_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

public class MACDOverRSILongExitStrategy1 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean currentPriceBelowSMA = realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex() - 1) > realTimeData.getCurrentPrice();
        if (currentPriceBelowSMA) {
            return new SellingInstructions(SELL_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
