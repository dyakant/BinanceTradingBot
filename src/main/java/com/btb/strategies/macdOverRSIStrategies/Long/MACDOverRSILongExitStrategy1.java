package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_MARKET;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Если текущая цена опустилась ниже SMA, закрыть полностью маркету.
 */
@Slf4j
public class MACDOverRSILongExitStrategy1 extends MACDOverRSIBaseExitStrategy {
    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean currentPriceBelowSMA = realTimeData.getCurrentPrice() < realTimeData.getSMAValueAtIndex(realTimeData.getLastCloseIndex());
        if (currentPriceBelowSMA) {
            log.info("{} MACDOverRSILongExitStrategy1 executed, currentPrice={}, SMAValueAtIndex({})={}", realTimeData.getSymbol(), realTimeData.getCurrentPrice(), realTimeData.getLastCloseIndex(), realTimeData.getSMAValueAtIndex(realTimeData.getLastCloseIndex()));
            return new SellingInstructions(CLOSE_LONG_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
