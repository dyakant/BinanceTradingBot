package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;

import static data.DataHolder.CandleType.OPEN;
import static data.DataHolder.CrossType.UP;
import static data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.SHORT_EXIT_OPEN_THRESHOLD;

@Slf4j
public class MACDOverRSIShortExitStrategy2 extends MACDOverRSIBaseExitStrategy {

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        boolean isOpenMacdCandleCrossedRsiUp = realTimeData.crossed(MACD_OVER_RSI, OPEN, UP, SHORT_EXIT_OPEN_THRESHOLD);
        if (isOpenMacdCandleCrossedRsiUp) {
            log.info("{} MACDOverRSIShortExitStrategy2 executed, currentMacdOverRsiValue={}, prevMacdOverRsiValue={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()));
            return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
        }
        return null;
    }
}
