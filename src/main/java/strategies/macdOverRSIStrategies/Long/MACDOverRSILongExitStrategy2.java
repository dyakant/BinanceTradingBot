package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static data.DataHolder.CandleType.OPEN;
import static data.DataHolder.CrossType.DOWN;
import static data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static positions.PositionHandler.ClosePositionTypes.SELL_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.LONG_EXIT_OPEN_THRESHOLD;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Если MACD пересёк RSI вниз с порогом, закрыть полностью по лимитной заявке.
 */
public class MACDOverRSILongExitStrategy2 extends MACDOverRSIBaseExitStrategy {
    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean isOpenMacdCandleCrossedRsiDown = realTimeData.crossed(MACD_OVER_RSI, OPEN, DOWN, LONG_EXIT_OPEN_THRESHOLD);
        if (isOpenMacdCandleCrossedRsiDown) {
            return new SellingInstructions(SELL_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
