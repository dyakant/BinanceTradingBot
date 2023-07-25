package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static com.btb.data.DataHolder.CandleType.OPEN;
import static com.btb.data.DataHolder.CrossType.DOWN;
import static com.btb.data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.LONG_EXIT_OPEN_THRESHOLD;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Если MACD пересёк RSI вниз с порогом, закрыть полностью по лимитной заявке.
 */
@Slf4j
public class MACDOverRSILongExitStrategy2 extends MACDOverRSIBaseExitStrategy {
    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean isOpenMacdCandleCrossedRsiDown = realTimeData.crossed(MACD_OVER_RSI, OPEN, DOWN, LONG_EXIT_OPEN_THRESHOLD);
        if (isOpenMacdCandleCrossedRsiDown) {
            log.info("{} MACDOverRSILongExitStrategy2 executed, currentMacdOverRsiValue={}, prevMacdOverRsiValue={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()));
            return new SellingInstructions(CLOSE_LONG_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
