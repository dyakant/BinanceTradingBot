package com.btb.strategies.rsiStrategies;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.ExitStrategy;

import static com.btb.data.RealTimeData.CandleType.OPEN;
import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_4_SELLING_PERCENTAGE;
import static com.btb.strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_4_UNDER_THRESHOLD;

public class RSIExitStrategy4 implements ExitStrategy {

    public SellingInstructions run(RealTimeData realTimeData) {
        if (!(realTimeData.isRSIValueAboveThreshold(OPEN, RSI_EXIT_OPTION_4_UNDER_THRESHOLD))) {
            return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_4_SELLING_PERCENTAGE);
        }
        return null;
    }
}
