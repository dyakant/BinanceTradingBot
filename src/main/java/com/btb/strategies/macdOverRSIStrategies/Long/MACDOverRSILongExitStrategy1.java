package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import lombok.extern.slf4j.Slf4j;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_MARKET;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Если текущая цена опустилась ниже SMA, закрыть полностью маркету.
 */
@Slf4j
public class MACDOverRSILongExitStrategy1 extends MACDOverRSIBaseExitStrategy {
    @Override
    public SellingInstructions run(RealTimeData data) {
        log.trace("{} Enter MACDOverRSILongExitStrategy1", data.getSymbol());
        double previousSMAValue = data.getSMAValueAtIndex(data.getLastIndex() - 1);
        boolean currentPriceBelowPreviousSMA = data.getCurrentPrice() < previousSMAValue;
        if (currentPriceBelowPreviousSMA) {
            log.info("{} MACDOverRSILongExitStrategy1 close a position", data.getSymbol());
            return new SellingInstructions(CLOSE_LONG_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
