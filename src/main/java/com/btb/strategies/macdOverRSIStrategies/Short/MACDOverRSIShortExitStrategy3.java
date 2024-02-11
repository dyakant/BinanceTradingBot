package com.btb.strategies.macdOverRSIStrategies.Short;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import com.btb.data.Trailer;
import lombok.extern.slf4j.Slf4j;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при шорте.
 * Начинает слежение если гистограмма негативна, но направление начало меняться.
 * Цена выхода будет обновляться.
 * Если гистограмма по-прежнему негативна, то ок.
 * Если гистограмма позитивна, и цена поднялась выше цены выхода, то закрыть позицию
 */
@Slf4j
public class MACDOverRSIShortExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(RealTimeData realTimeData) {
        log.trace("{} Enter MACDOverRSIShortExitStrategy3, isTrailing={}", realTimeData.getSymbol(), isTrailing);
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateExitPrice(currentPrice);
            if (isNegativeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSIShortExitStrategy3 histogram is negative, break trailing", realTimeData.getSymbol());
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSIShortExitStrategy3 close a position", realTimeData.getSymbol());
                return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            } else {
                log.trace("{} MACDOverRSIShortExitStrategy3 positive histogram, not a time to sell", realTimeData.getSymbol());
            }
        } else {
            if (isDirectionChangedAndNegativeThreeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSIShortExitStrategy3 direction changed to positive, start trailing", realTimeData.getSymbol());
                trailer.setAbsoluteMaxPrice(currentPrice);
                isTrailing = true;
            }
        }
        return null;
    }
}
