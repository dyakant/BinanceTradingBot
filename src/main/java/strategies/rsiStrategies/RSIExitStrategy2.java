package strategies.rsiStrategies;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.ExitStrategy;
import strategies.PositionInStrategy;

import static data.DataHolder.CandleType.CLOSE;
import static data.DataHolder.IndicatorType.RSI;
import static positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static strategies.PositionInStrategy.*;
import static strategies.rsiStrategies.RSIConstants.*;

public class RSIExitStrategy2 implements ExitStrategy {
    private PositionInStrategy positionInStrategy = POSITION_ONE;

    /**
     * Checks if the current close of RSIIndicator value is above 73, and then below 70 and then below 60.
     *
     * @param realTimeData
     * @return the percentage of quantity to sell, null otherwise.
     */
    public SellingInstructions run(DataHolder realTimeData) {
        if (positionInStrategy == POSITION_ONE) {
            if (realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_2_OVER_THRESHOLD1)) {
                positionInStrategy = POSITION_TWO;
            }
            return null;
        } else if (positionInStrategy == POSITION_TWO) {
            if (!realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_2_UNDER_THRESHOLD1)) {
                positionInStrategy = POSITION_THREE;
                return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_2_SELLING_PERCENTAGE1);

            }
        } else if (positionInStrategy == POSITION_THREE) {
            if (!realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_2_UNDER_THRESHOLD2)) {
                positionInStrategy = POSITION_ONE;
                return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_2_SELLING_PERCENTAGE2);
            }
        }
        return null;
    }
}
