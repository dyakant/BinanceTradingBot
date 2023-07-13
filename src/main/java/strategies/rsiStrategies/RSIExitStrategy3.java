package strategies.rsiStrategies;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.ExitStrategy;

import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.rsiStrategies.RSIConstants.RSI_EXIT_OPTION_3_SELLING_PERCENTAGE;

public class RSIExitStrategy3 implements ExitStrategy {
    private double rsiValueTwoBefore = -1.0;
    private double rsiValueBefore;
    private boolean firstTime = true;

    public SellingInstructions run(DataHolder realTimeData) {
        if (firstTime) {
            rsiValueBefore = realTimeData.getRsiCloseValue(); // last closed candle rsi value
            firstTime = false;
        } // not the first time. already ran.
        double rsiValue = realTimeData.getRsiOpenValue();
        if (rsiValueBefore != realTimeData.getRsiCloseValue()) {
            updateValues(realTimeData.getRsiCloseValue());
        }
        if (lostValueOf15(rsiValueBefore, rsiValue)) {
            return new SellingInstructions(SELL_LIMIT, RSI_EXIT_OPTION_3_SELLING_PERCENTAGE, this.getClass().getName());

        }
        if (rsiValueTwoBefore != -1.0 && lostValueOf15(rsiValueTwoBefore, rsiValue)) {
            //TODO: no logs, no different, same result
            return new SellingInstructions(SELL_LIMIT, RSI_EXIT_OPTION_3_SELLING_PERCENTAGE, this.getClass().getName());
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
