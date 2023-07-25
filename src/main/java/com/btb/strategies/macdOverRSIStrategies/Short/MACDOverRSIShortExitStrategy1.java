package com.btb.strategies.macdOverRSIStrategies.Short;

import com.btb.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import com.btb.positions.SellingInstructions;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static com.btb.positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_MARKET;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSIShortExitStrategy1 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double smaValue = realTimeData.getSMAValueAtIndex(realTimeData.getLastCloseIndex());
        boolean currentPriceAboveSMA = smaValue < realTimeData.getCurrentPrice();
        if (currentPriceAboveSMA) {
            log.info("{} MACDOverRSIShortExitStrategy1 executed, currentPrice={}, SMAValueAtIndex({})={}", realTimeData.getSymbol(), realTimeData.getCurrentPrice(), realTimeData.getLastCloseIndex(), smaValue);
            return new SellingInstructions(CLOSE_SHORT_MARKET, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
