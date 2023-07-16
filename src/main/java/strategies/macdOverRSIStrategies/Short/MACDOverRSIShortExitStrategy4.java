package strategies.macdOverRSIStrategies.Short;

import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import positions.SellingInstructions;
import strategies.macdOverRSIStrategies.MACDOverRSIBaseExitStrategy;
import utils.Trailer;

import static positions.PositionHandler.ClosePositionTypes.CLOSE_SHORT_LIMIT;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE;

@Slf4j
public class MACDOverRSIShortExitStrategy4 extends MACDOverRSIBaseExitStrategy {

    private boolean isTrailing = false;
    private final Trailer trailer;

    public MACDOverRSIShortExitStrategy4(Trailer trailer) {
        this.trailer = trailer;
    }

    @Override
    public SellingInstructions run(DataHolder realTimeData) {
        if (isTrailing) {
            double currentPrice = realTimeData.getCurrentPrice();
            trailer.updateTrailer(currentPrice);
            if (changedDirectionAndPositiveThreeHistogram(realTimeData)) {
                log.info("{} MACDOverRSIShortExitStrategy4 change trailing false, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = false;
                return null;
            }
            if (trailer.needToSell(currentPrice)) {
                log.info("{} MACDOverRSIShortExitStrategy4 executed, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                return new SellingInstructions(CLOSE_SHORT_LIMIT, MACD_OVER_RSI_EXIT_SELLING_PERCENTAGE);
            }
        } else {
            if (stayInTrackAndThreePositiveHistograms(realTimeData)) {
                log.info("{} MACDOverRSIShortExitStrategy4 change trailing true, cur={}, prev={}, third={}", realTimeData.getSymbol(), realTimeData.getMacdOverRsiCloseValue(), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex()), realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2));
                isTrailing = true;
                trailer.setAbsoluteMaxPrice(realTimeData.getCurrentPrice());
            }
        }
        return null;
    }
}
