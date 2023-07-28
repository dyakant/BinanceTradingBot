package com.btb.strategies.macdOverRSIStrategies.Short;

import com.btb.data.DataHolder;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import lombok.extern.slf4j.Slf4j;

import static com.btb.data.DataHolder.CandleType.OPEN;
import static com.btb.data.DataHolder.CrossType.UP;
import static com.btb.data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.SHORT_EXIT_OPEN_THRESHOLD;

/**
 * Стратегия для закрытия позиции при шорте.
 * Если MACD пересёк RSI вверх, с порогом, закрыть полностью по лимитной заявке.
 */
@Slf4j
public class MACDOverRSIShortExitStrategy2 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        log.trace("{} Enter MACDOverRSIShortExitStrategy2", realTimeData.getSymbol());
        boolean isOpenMacdCandleCrossedRsiUp = realTimeData.crossed(MACD_OVER_RSI, OPEN, UP, SHORT_EXIT_OPEN_THRESHOLD);
        if (isOpenMacdCandleCrossedRsiUp) {
            log.info("{} MACDOverRSIShortExitStrategy2 close a position", realTimeData.getSymbol());
            return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
