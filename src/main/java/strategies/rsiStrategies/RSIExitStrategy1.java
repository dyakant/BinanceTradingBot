package strategies.rsiStrategies;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.ExitStrategy;
import strategies.PositionInStrategy;

import static data.DataHolder.CandleType.CLOSE;
import static data.DataHolder.IndicatorType.RSI;
import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.PositionInStrategy.*;
import static strategies.rsiStrategies.RSIConstants.*;

public class RSIExitStrategy1 implements ExitStrategy {
    private PositionInStrategy positionInStrategy = POSITION_ONE;

    public SellingInstructions run(DataHolder realTimeData) {
        if (positionInStrategy == POSITION_ONE) {
            if (realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_1_OVER_THRESHOLD1)
                    && !(realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_2_OVER_THRESHOLD1))) {
                positionInStrategy = POSITION_TWO;
            }
            return null;
        } else if (positionInStrategy == POSITION_TWO) {
            if (!realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_1_UNDER_THRESHOLD1)) {
                positionInStrategy = POSITION_THREE;
                return new SellingInstructions(SELL_LIMIT, RSI_EXIT_OPTION_1_SELLING_PERCENTAGE1, this.getClass().getName());
            }
        } else if (positionInStrategy == POSITION_THREE) {
            if (!realTimeData.above(RSI, CLOSE, RSI_EXIT_OPTION_1_UNDER_THRESHOLD2)) {
                positionInStrategy = POSITION_ONE;
                return new SellingInstructions(SELL_LIMIT, RSI_EXIT_OPTION_1_SELLING_PERCENTAGE2, this.getClass().getName());
            }
        }
        return null;
    }
}
