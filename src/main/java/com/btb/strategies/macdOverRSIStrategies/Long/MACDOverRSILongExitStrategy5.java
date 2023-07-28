package com.btb.strategies.macdOverRSIStrategies.Long;

import com.btb.data.DataHolder;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import com.btb.utils.Trailer;
import lombok.extern.slf4j.Slf4j;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_LONG_MARKET;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

/**
 * Стратегия для закрытия позиции при лонге.
 * "Страховочная", с высоким trailingPercentage
 * Просто следит, чтобы цена не упала больше, чем на дельту с trailingPercentage
 */
@Slf4j
public class MACDOverRSILongExitStrategy5 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy5(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        log.trace("{} Enter MACDOverRSILongExitStrategy5, isTrailing={}", realTimeData.getSymbol(), isTrailing);
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateExitPrice(currentPrice);
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy5 close a position", realTimeData.getSymbol());
                return new SellingInstructions(CLOSE_LONG_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            } else {
                log.trace("{} MACDOverRSILongExitStrategy5 not a time to sell", realTimeData.getSymbol());
            }
        } else {
            log.trace("{} MACDOverRSILongExitStrategy5 start trailing", realTimeData.getSymbol());
            trailer.setAbsoluteMaxPrice(currentPrice);
            isTrailing = true;
        }
        return null;
    }

}
