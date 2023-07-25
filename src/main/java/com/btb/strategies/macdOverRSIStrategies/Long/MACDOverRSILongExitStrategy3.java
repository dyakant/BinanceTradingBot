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
 */
@Slf4j
public class MACDOverRSILongExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSILongExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateTrailer(currentPrice);
            if (stayInTrackAndThreePositiveHistograms(realTimeData)) {
                log.info("{} MACDOverRSILongExitStrategy3 change trailing false, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSILongExitStrategy3 executed, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(CLOSE_LONG_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        } else {
            if (changedDirectionAndPositiveThreeHistogram(realTimeData)) {
                trailer.setAbsoluteMaxPrice(currentPrice);
                log.info("{} MACDOverRSILongExitStrategy3 change trailing true, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = true;
            }
        }
        return null;
    }
}
