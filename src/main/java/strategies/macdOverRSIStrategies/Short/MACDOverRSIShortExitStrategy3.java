package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSIShortExitStrategy3 extends MACDOverRSIBaseExitStrategy {
    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy3(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        if (isTrailing) {
            trailer.updateTrailer(currentPrice);
            if (stayInTrackAndThreeNegativeHistograms(realTimeData)) {
                log.info("{} MACDOverRSIShortExitStrategy3 change trailing false, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSIShortExitStrategy3 executed, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        } else {
            if (changedDirectionAndNegativeThreeHistogram(realTimeData)) {
                log.info("{} MACDOverRSIShortExitStrategy3 change trailing true, current={}, previous={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = true;
                trailer.setAbsoluteMaxPrice(currentPrice);
            }
        }
        return null;
    }
}
