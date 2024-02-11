package com.btb.strategies.rsiStrategies;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.ExitStrategy;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_3_SELLING_PERCENTAGE;

public class RSIExitStrategy3 implements ExitStrategy {
    private double rsiValueTwoBefore = -1.0;
    private double rsiValueBefore;
    private boolean firstTime = true;

    public SellingInstructions run(RealTimeData data) {
        double currRSIValue = data.getRSIValueAtIndex(data.getLastIndex());
        double prevRSIValue = data.getRSIValueAtIndex(data.getLastIndex() - 1);
        if (firstTime) {
            rsiValueBefore = prevRSIValue; // last closed candle rsi value
            firstTime = false;
        } // not the first time. already ran.
        if (rsiValueBefore != prevRSIValue) {
            updateValues(prevRSIValue);
        }
        if (lostValueOf15(rsiValueBefore, currRSIValue)) {
            return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_3_SELLING_PERCENTAGE);

        }
        if (rsiValueTwoBefore != -1.0 && lostValueOf15(rsiValueTwoBefore, currRSIValue)) {
            //TODO: no logs, no different, same result
            return new SellingInstructions(CLOSE_LONG_LIMIT, RSI_EXIT_OPTION_3_SELLING_PERCENTAGE);
        }
        return null;
    }

    private boolean lostValueOf15(double oldVal, double newVal) {
        return oldVal - newVal >= 15;
    }

    private void updateValues(double newValue) {
        double temp = rsiValueBefore;
        rsiValueBefore = newValue;
        rsiValueTwoBefore = temp;
    }
}
