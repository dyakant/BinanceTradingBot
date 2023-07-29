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
 * Начинает слежение если гистограмма позитивна (в трёх точках).
 * Цена выхода будет обновляться.
 * Если при позитивной гистограмме направление поменялось, то ок, остановить слежение.
 * Иначе, если цена поднялась выше цены выхода, то закрыть позицию
 */
@Slf4j
public class MACDOverRSIShortExitStrategy4 extends MACDOverRSIBaseExitStrategy {

    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(RealTimeData realTimeData) {
        log.trace("{} Enter MACDOverRSIShortExitStrategy4, isTrailing={}", realTimeData.getSymbol(), isTrailing);
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateExitPrice(currentPrice);
            if (isDirectionChangedAndPositiveThreeHistogram(realTimeData)) {
                log.trace("{} MACDOverRSIShortExitStrategy4 direction changed to negative, stop trailing", realTimeData.getSymbol());
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSIShortExitStrategy4 close a position", realTimeData.getSymbol());
                return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            } else {
                log.trace("{} MACDOverRSIShortExitStrategy4 direction is positive, not a time to sell", realTimeData.getSymbol());
            }
        } else {
            if (isPositiveHistogram(realTimeData)) {
                log.trace("{} MACDOverRSIShortExitStrategy4 histogram is positive, start trailing", realTimeData.getSymbol());
                isTrailing = true;
                trailer.setAbsoluteMaxPrice(currentPrice);
            }
        }
        return null;
    }
}
