package strategies.macdOverRSIStrategies.Long;

import data.DataHolder;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static positions.PositionHandler.ClosePositionTypes.SELL_MARKET;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Если текущая цена опустилась ниже SMA, закрыть полностью маркету.
 */
public class MACDOverRSILongExitStrategy1 extends MACDOverRSIBaseExitStrategy {
    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean currentPriceBelowSMA = realTimeData.getCurrentPrice() < realTimeData.getSMAValueAtIndex(realTimeData.getLastCloseIndex());
        if (currentPriceBelowSMA) {
            return new SellingInstructions(SELL_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE, this.getClass().getName());
        }
        return null;
    }
}
