package com.btb.strategies.macdOverRSIStrategies.Short;

import com.btb.data.DataHolder;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import lombok.extern.slf4j.Slf4j;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_MARKET;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при шорте.
 * Если текущая цена поднялась выше SMA, закрыть позицию.
 */
@Slf4j
public class MACDOverRSIShortExitStrategy1 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        log.trace("{} Enter MACDOverRSIShortExitStrategy1", realTimeData.getSymbol());
        double previousSMAValue = realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex() - 1);
        boolean currentPriceAbovePreviousSMA = realTimeData.getCurrentPrice() > previousSMAValue;
        if (currentPriceAbovePreviousSMA) {
            log.info("{} MACDOverRSIShortExitStrategy1 close a position", realTimeData.getSymbol());
            return new SellingInstructions(CLOSE_SHORT_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
