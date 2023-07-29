package com.btb.strategies.rsiStrategies;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.ExitStrategy;
import com.btb.strategies.PositionInStrategy;

import static com.btb.data.RealTimeData.CandleType.CLOSE;
import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.PositionInStrategy.*;
import static com.btb.strategies.rsiStrategies.RSIConstants.*;

public class RSIExitStrategy1 implements ExitStrategy {
    private PositionInStrategy positionInStrategy = POSITION_ONE;

    public SellingInstructions run(RealTimeData realTimeData) {
        if (positionInStrategy == POSITION_ONE) {
            if (realTimeData.isRSIValueAboveThreshold(CLOSE, RSI_EXIT_OPTION_1_OVER_THRESHOLD1)
                    && !(realTimeData.isRSIValueAboveThreshold(CLOSE, RSI_EXIT_OPTION_2_OVER_THRESHOLD1))) {
                positionInStrategy = POSITION_TWO;
            }
            return null;
        } else if (positionInStrategy == POSITION_TWO) {
            if (!realTimeData.isRSIValueAboveThreshold(CLOSE, RSI_EXIT_OPTION_1_UNDER_THRESHOLD1)) {
                positionInStrategy = POSITION_THREE;
                return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_1_SELLING_PERCENTAGE1);
            }
        } else if (positionInStrategy == POSITION_THREE) {
            if (!realTimeData.isRSIValueAboveThreshold(CLOSE, RSI_EXIT_OPTION_1_UNDER_THRESHOLD2)) {
                positionInStrategy = POSITION_ONE;
                return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_1_SELLING_PERCENTAGE2);
            }
        }
        return null;
    }
}
