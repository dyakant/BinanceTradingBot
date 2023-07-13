package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static data.DataHolder.CandleType.OPEN;
import static data.DataHolder.CrossType.UP;
import static data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.SHORT_EXIT2_OPEN_THRESHOLD;

public class MACDOverRSIShortExitStrategy2 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean openCrossed03 = realTimeData.crossed(MACD_OVER_RSI, UP, OPEN, SHORT_EXIT2_OPEN_THRESHOLD);
        if (openCrossed03) {
            return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
