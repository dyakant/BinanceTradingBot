package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.DataHolder;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import com.btb.utils.Trailer;
import lombok.extern.slf4j.Slf4j;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Начинает слежение если гистограмма позитивна, но направление начало меняться.
 * Цена выхода будет обновляться.
 * Если гистограмма по-прежнему позитивна, то ок.
 * Если гистограмма негативна, и цена упала ниже цены выхода, то закрыть позицию
 */
@Slf4j
public class MACDOverRSILongExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private final Trailer trailer;
    private boolean isTrailing = false;

    public MACDOverRSILongExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        log.trace("{} Enter MACDOverRSILongExitStrategy3, isTrailing={}", realTimeData.getSymbol(), isTrailing);
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateExitPrice(currentPrice);
            if (isPositiveHistogram(realTimeData)) {
                log.trace("{} MACDOverRSILongExitStrategy3 histogram is positive, break trailing", realTimeData.getSymbol());
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy3 close a position", realTimeData.getSymbol());
                return new SellingInstructions(CLOSE_LONG_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            } else {
                log.trace("{} MACDOverRSILongExitStrategy3 negative histogram, not a time to sell", realTimeData.getSymbol());
            }
        } else {
            if (isDirectionChangedAndPositiveThreeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSILongExitStrategy3 direction changed to negative, start trailing", realTimeData.getSymbol());
                trailer.setAbsoluteMaxPrice(currentPrice);
                isTrailing = true;
            }
        }
        return null;
    }
}
