package data;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.event.CandlestickEvent;
import com.binance.client.model.market.Candlestick;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import singletonHelpers.RequestClient;
import strategies.rsiStrategies.RSIConstants;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.*;
import static utils.Utils.getZonedDateTime;

//* For us, in realTimeData, the last candle is always open. The previous ones are closed.
@Slf4j
public class RealTimeData {
    public static final int UPDATE_DATA_SKIP_EVENTS_COUNTER = 20;
    private Long lastCandleOpenTime;
    private BaseBarSeries baseBarSeries;
    private double currentPrice;
    private RSIIndicator rsiIndicator;
    private MACDIndicator macdOverRsiIndicator;
    private SMAIndicator smaIndicator;
    private int updateCounter = 0;

    public RealTimeData(String symbol, CandlestickInterval interval) {
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
     * of realTimeData and erase the first. If the candle is open - delete the last one from real time data and push the new one.
     * Calculates the RSIIndicators in either case - to get the most accurate data.
     * to realTimeData
     *
     * @param event - the new Candlestick received from the subscribeCandleStickEvent.
     */
    public synchronized DataHolder updateData(String symbol, CandlestickEvent event) {
        boolean isNewCandle = updateLastCandle(event);
        log.info("{} CandlestickEvent received {}, isNewCandle={}, eventData: {}", symbol, updateCounter, isNewCandle, event);
        if (!isNewCandle && updateCounter++ != UPDATE_DATA_SKIP_EVENTS_COUNTER) {
            return null;
        }
        log.info("{} CandlestickEvent continue: \nbar[{}]: {}, \nbar[{}]: {}, \nbar[{}]: {}", symbol,
                baseBarSeries.getEndIndex(), baseBarSeries.getBar(baseBarSeries.getEndIndex()),
                baseBarSeries.getEndIndex() - 1, baseBarSeries.getBar(baseBarSeries.getEndIndex() - 1),
                baseBarSeries.getEndIndex() - 2, baseBarSeries.getBar(baseBarSeries.getEndIndex() - 2));
        updateCounter = 0;
        calculateIndicators();
        log.info("{} CandlestickEvent indicators. rsiIndicator: {}, macdOverRsiIndicator: {}, smaIndicator: {}", symbol, rsiIndicator, macdOverRsiIndicator, smaIndicator);
        return new DataHolder(symbol, currentPrice, rsiIndicator, macdOverRsiIndicator, smaIndicator, baseBarSeries.getEndIndex());
    }

    private boolean updateLastCandle(CandlestickEvent event) {
        currentPrice = event.getClose().doubleValue();
        boolean isNewCandle = !(event.getStartTime().equals(lastCandleOpenTime));
        ZonedDateTime closeTime = getZonedDateTime(event.getCloseTime());
        Duration candleDuration = Duration.ofMillis(event.getCloseTime() - event.getStartTime());
        double open = event.getOpen().doubleValue();
        double high = event.getHigh().doubleValue();
        double low = event.getLow().doubleValue();
        double close = event.getClose().doubleValue();
        double volume = event.getVolume().doubleValue();
        lastCandleOpenTime = event.getStartTime();
        if (isNewCandle) {
            baseBarSeries = baseBarSeries.getSubSeries(1, baseBarSeries.getEndIndex() + 1);
        } else {
            baseBarSeries = baseBarSeries.getSubSeries(0, baseBarSeries.getEndIndex());
        }
        baseBarSeries.addBar(candleDuration, closeTime, open, high, low, close, volume);
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
    }

    private void calculateIndicators() {
        rsiIndicator = calculateRSI(RSIConstants.RSI_CANDLE_NUM);
        macdOverRsiIndicator = calculateMacdOverRsi();
        smaIndicator = new SMAIndicator(new ClosePriceIndicator(baseBarSeries), SMA_CANDLE_NUM);
    }

    private RSIIndicator calculateRSI(int candleNum) {
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(baseBarSeries);
        return new RSIIndicator(closePriceIndicator, candleNum);
    }

    private MACDIndicator calculateMacdOverRsi() {
        RSIIndicator rsiIndicator = calculateRSI(RSI_CANDLE_NUM);
        return new MACDIndicator(rsiIndicator, FAST_BAR_COUNT, SLOW_BAR_COUNT);
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

}
