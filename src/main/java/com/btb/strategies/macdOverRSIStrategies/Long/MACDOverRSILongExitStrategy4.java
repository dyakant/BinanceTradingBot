package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import com.btb.utils.Trailer;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_LIMIT;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * Начинает слежение если гистограмма негативна (в трёх точках).
 * Цена выхода будет обновляться.
 * Если при негативной гистограмме направление поменялось, то остановить слежение.
 * Иначе, если цена упала ниже цены выхода, то закрыть позицию
 */
@Slf4j
public class MACDOverRSILongExitStrategy4 extends MACDOverRSIBaseExitStrategy {
    private final Trailer trailer;
    private boolean isTrailing = false;

    public MACDOverRSILongExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        log.trace("{} Enter MACDOverRSILongExitStrategy4, isTrailing={}", realTimeData.getSymbol(), isTrailing);
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateExitPrice(currentPrice);
            if (isDirectionChangedAndNegativeThreeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSILongExitStrategy4 direction changed to positive, stop trailing", realTimeData.getSymbol());
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy4 close a position", realTimeData.getSymbol());
                return new SellingInstructions(CLOSE_LONG_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            } else {
                log.trace("{} MACDOverRSILongExitStrategy4 direction is negative, not a time to sell", realTimeData.getSymbol());
            }
        } else {
            if (isNegativeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSILongExitStrategy4 histogram is negative, start trailing", realTimeData.getSymbol());
                trailer.setAbsoluteMaxPrice(currentPrice);
                isTrailing = true;
            }
        }
        return null;
    }
}
