package strategies.rsiStrategies;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.ExitStrategy;

import static data.DataHolder.CandleType.OPEN;
import static data.DataHolder.IndicatorType.RSI;
import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_4_SELLING_PERCENTAGE;
import static strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_4_UNDER_THRESHOLD;

public class RSIExitStrategy4 implements ExitStrategy {

    public SellingInstructions run(DataHolder realTimeData) {
        if (!(realTimeData.above(RSI, OPEN, RSI_EXIT_OPTION_4_UNDER_THRESHOLD))) {
            return new SellingInstructions(SELL_LIMIT, RSI_EXIT_OPTION_4_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
