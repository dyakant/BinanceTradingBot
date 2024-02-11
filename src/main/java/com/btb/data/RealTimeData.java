package com.btb.data;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.event.CandlestickEvent;
import com.binance.client.model.market.Candlestick;
import com.btb.singletonHelpers.RequestClient;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants;
import com.btb.strategies.rsiStrategies.RSIConstants;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.*;
import static com.btb.utils.Utils.getZonedDateTime;

//* For us, in realTimeData, the last candle is always open. The previous ones are closed.
@Slf4j
public class RealTimeData {
    public static final int UPDATE_DATA_SKIP_EVENTS_COUNTER = 20;
    private final String symbol;
    private final CandlestickInterval interval;
    private Long lastCandleOpenTime;
    private BaseBarSeries baseBarSeries;
    private int endIndex;
    private Double currentPrice;
    private ClosePriceIndicator closePriceIndicator;
    private RSIIndicator rsiIndicator;
    private MACDIndicator macdOverRsiIndicator;
    private SMAIndicator smaIndicator;
    private EMAIndicator emaIndicator;
    private int updateCounter = 0;

    public RealTimeData(String symbol, CandlestickInterval interval) {
        this.symbol = symbol;
        this.interval = interval;
        baseBarSeries = new BaseBarSeries();
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        List<Candlestick> candlestickBars = syncRequestClient.getCandlestick(symbol, interval, null, null, Config.CANDLE_NUM);
        lastCandleOpenTime = candlestickBars.get(candlestickBars.size() - 1).getOpenTime();
        currentPrice = candlestickBars.get(candlestickBars.size() - 1).getClose().doubleValue();
        fillRealTimeData(candlestickBars);
        calculateIndicators();
    }

    /**
     * Receives the current candlestick - usually an open one.
     * The function updateData updates realTimeData in the following way: if the candle received is closed => push to the end
     * of realTimeData and erase the first. If the candle is open - delete the last one from real time com.btb.data and push the new one.
     * Calculates the RSIIndicators in either case - to get the most accurate com.btb.data.
     * to realTimeData
     *
     * @param event - the new Candlestick received from the subscribeCandleStickEvent.
     */
    public synchronized boolean updateData(String symbol, CandlestickEvent event) {
        boolean isNewCandle = updateLastCandle(event);
        if (!isNewCandle && updateCounter++ != UPDATE_DATA_SKIP_EVENTS_COUNTER) {
            return false;
        }
        updateCounter = 0;
        calculateIndicators();
        logTraceBarsState();
        return true;
    }

    private void logTraceBarsState() {
        if (log.isTraceEnabled()) {
            log.trace("{} CandlestickEvent updated: \nbar[{}]: {}, \nbar[{}]: {}, \nbar[{}]: {}", symbol,
                    baseBarSeries.getEndIndex(), baseBarSeries.getBar(baseBarSeries.getEndIndex()),
                    baseBarSeries.getEndIndex() - 1, baseBarSeries.getBar(baseBarSeries.getEndIndex() - 1),
                    baseBarSeries.getEndIndex() - 2, baseBarSeries.getBar(baseBarSeries.getEndIndex() - 2));
        }
    }

    private boolean updateLastCandle(CandlestickEvent event) {
        currentPrice = event.getClose().doubleValue();
        boolean isNewCandle = !(event.getStartTime().equals(lastCandleOpenTime));
        lastCandleOpenTime = event.getStartTime();
        ZonedDateTime closeTime = getZonedDateTime(event.getCloseTime());
        Duration candleDuration = Duration.ofMillis(event.getCloseTime() - event.getStartTime());
        double open = event.getOpen().doubleValue();
        double high = event.getHigh().doubleValue();
        double low = event.getLow().doubleValue();
        double close = event.getClose().doubleValue();
        double volume = event.getVolume().doubleValue();
        if (isNewCandle) {
            baseBarSeries = baseBarSeries.getSubSeries(1, baseBarSeries.getEndIndex() + 1);
        } else {
            baseBarSeries = baseBarSeries.getSubSeries(0, baseBarSeries.getEndIndex());
        }
        baseBarSeries.addBar(candleDuration, closeTime, open, high, low, close, volume);
        endIndex = baseBarSeries.getEndIndex();
        return isNewCandle;
    }

    private void fillRealTimeData(List<Candlestick> candlestickBars) {
        for (Candlestick candlestickBar : candlestickBars) {
            ZonedDateTime closeTime = getZonedDateTime(candlestickBar.getCloseTime());
            Duration candleDuration = Duration.ofMillis(candlestickBar.getCloseTime() - candlestickBar.getOpenTime());
            double open = candlestickBar.getOpen().doubleValue();
            double high = candlestickBar.getHigh().doubleValue();
            double low = candlestickBar.getLow().doubleValue();
            double close = candlestickBar.getClose().doubleValue();
            double volume = candlestickBar.getVolume().doubleValue();
            baseBarSeries.addBar(candleDuration, closeTime, open, high, low, close, volume);
        }
        endIndex = baseBarSeries.getEndIndex();
    }

    private void calculateIndicators() {
        closePriceIndicator = new ClosePriceIndicator(baseBarSeries);
        smaIndicator = new SMAIndicator(closePriceIndicator, SMA_CANDLE_NUM);
        rsiIndicator = new RSIIndicator(closePriceIndicator, RSIConstants.RSI_CANDLE_NUM);
        RSIIndicator rsiForMACDIndicator = new RSIIndicator(closePriceIndicator, MACDOverRSIConstants.RSI_CANDLE_NUM);
        macdOverRsiIndicator = new MACDIndicator(rsiForMACDIndicator, FAST_BAR_COUNT, SLOW_BAR_COUNT);
        emaIndicator = new EMAIndicator(macdOverRsiIndicator, SIGNAL_LENGTH);
    }

    public double getMACDOverRsiValueAtIndex(int index) {
        // TODO What does it mean?
        double macdOverRsiMacdLineValue = macdOverRsiIndicator.getValue(index).doubleValue();
        double macdOverRsiSignalLineValue = emaIndicator.getValue(index).doubleValue();
        return macdOverRsiMacdLineValue - macdOverRsiSignalLineValue;
    }

    public double getRSIValueAtIndex(int index) {
        return rsiIndicator.getValue(index).doubleValue();
    }

    public double getSMAValueAtIndex(int index) {
        return smaIndicator.getValue(index).doubleValue();
    }

    public boolean isRSIValueAboveThreshold(CandleType type, int threshold) {
        return (type == CandleType.OPEN)
                ? getRSIValueAtIndex(endIndex) > threshold
                : getRSIValueAtIndex(endIndex - 1) > threshold;
    }

    public boolean isMACDOverRSIValueAboveThreshold(CandleType type, int threshold) {
        return (type == CandleType.OPEN)
                ? getMACDOverRsiValueAtIndex(endIndex) > threshold
                : getMACDOverRsiValueAtIndex(endIndex - 1) > threshold;
    }

    public boolean isSMAValueAboveThreshold(CandleType type, int threshold) {
        return (type == CandleType.OPEN)
                ? getSMAValueAtIndex(endIndex) > threshold
                : getSMAValueAtIndex(endIndex - 1) > threshold;
    }

    public boolean macdOverRsiCrossed(CandleType candleType, CrossType crossType, double threshold) {
        double currentMacdOverRsiValue, prevMacdOverRsiValue;
        if (candleType == CandleType.OPEN) {
            currentMacdOverRsiValue = getMACDOverRsiValueAtIndex(endIndex);
            prevMacdOverRsiValue = getMACDOverRsiValueAtIndex(endIndex - 1);
        } else {
            currentMacdOverRsiValue = getMACDOverRsiValueAtIndex(endIndex - 1);
            prevMacdOverRsiValue = getMACDOverRsiValueAtIndex(endIndex - 2);
        }
        return crossType == CrossType.UP
                ? currentMacdOverRsiValue > threshold && prevMacdOverRsiValue <= threshold
                : currentMacdOverRsiValue < threshold && prevMacdOverRsiValue >= threshold;
    }

    public boolean rsiCrossed(CandleType candleType, CrossType crossType, double threshold) {
        double rsiValueNow, rsiValuePrev;
        if (candleType == CandleType.OPEN) {
            rsiValueNow = getRSIValueAtIndex(endIndex);
            rsiValuePrev = getRSIValueAtIndex(endIndex - 1);
        } else {
            rsiValueNow = getRSIValueAtIndex(endIndex - 1);
            rsiValuePrev = getRSIValueAtIndex(endIndex - 2);
        }
        return crossType == CrossType.UP
                ? rsiValueNow > threshold && rsiValuePrev <= threshold
                : rsiValueNow < threshold && rsiValuePrev >= threshold;
    }

    public String getSymbol() {
        return symbol;
    }

    public CandlestickInterval getInterval() {
        return interval;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public int getLastIndex() {
        return endIndex;
    }

    public enum CandleType {
        OPEN, CLOSE
    }

    public enum CrossType {
        UP, DOWN
    }

}
