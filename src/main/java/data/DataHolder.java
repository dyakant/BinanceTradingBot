package data;

import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;

import static data.DataHolder.CandleType.OPEN;
import static data.DataHolder.CrossType.UP;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.SIGNAL_LENGTH;

public class DataHolder {
    private final Double currentPrice;
    private final RSIIndicator rsiIndicator;
    private final MACDIndicator macdOverRsiIndicator;
    private final double macdOverRsiCloseValue;
    private final SMAIndicator smaIndicator;
    private final int endIndex;

    public DataHolder(double currentPrice, RSIIndicator rsiIndicator, MACDIndicator macdOverRsiIndicator, SMAIndicator smaIndicator, int endIndex) {
        this.currentPrice = currentPrice;
        this.rsiIndicator = rsiIndicator;
        this.macdOverRsiIndicator = macdOverRsiIndicator;
        this.smaIndicator = smaIndicator;
        this.endIndex = endIndex;
        this.macdOverRsiCloseValue = getMacdOverRsiValueAtIndex(endIndex - 1);
    }

    public boolean above(IndicatorType indicatorType, CandleType type, int threshold) {
        if (indicatorType == IndicatorType.RSI) {
            if (type == OPEN) {
                return getRsiOpenValue() > threshold;
            } else {
                return getRsiCloseValue() > threshold;
            }
        } else if (indicatorType == IndicatorType.MACD_OVER_RSI) {
            if (type == OPEN) {
                return getMacdOverRsiValueAtIndex(endIndex) > threshold;
            } else {
                return getMacdOverRsiValueAtIndex(endIndex - 1) > threshold;
            }
        } else {
            if (type == OPEN) {
                return getSMAValueAtIndex(endIndex) > threshold;
            } else {
                return getSMAValueAtIndex(endIndex - 1) > threshold;
            }
        }
    }

    public boolean crossed(IndicatorType indicatorType, CandleType candleType, CrossType crossType, double threshold) {
        return switch (indicatorType) {
            case RSI -> rsiCrossed(candleType, crossType, threshold);
            case MACD_OVER_RSI -> macdOverRsiCrossed(candleType, crossType, threshold);
            default -> true; // will not come to this!
        };
    }

    public double getMacdOverRsiValueAtIndex(int index) {
        return getMacdOverRsiMacdLineValueAtIndex(index) - getMacdOverRsiSignalLineValueAtIndex(index);
    }

    private double getMacdOverRsiMacdLineValueAtIndex(int index) {
        return macdOverRsiIndicator.getValue(index).doubleValue();
    }

    private double getMacdOverRsiSignalLineValueAtIndex(int index) {
        EMAIndicator signal = new EMAIndicator(macdOverRsiIndicator, SIGNAL_LENGTH);
        return signal.getValue(index).doubleValue();
    }

    public double getSMAValueAtIndex(int index) {
        return smaIndicator.getValue(index).doubleValue();
    }

    private boolean macdOverRsiCrossed(CandleType candleType, CrossType crossType, double threshold) {
        double currentMacdOverRsiValue, prevMacdOverRsiValue;
        if (candleType == OPEN) {
            currentMacdOverRsiValue = getMacdOverRsiValueAtIndex(endIndex);
            prevMacdOverRsiValue = macdOverRsiCloseValue;
        } else {
            currentMacdOverRsiValue = macdOverRsiCloseValue;
            prevMacdOverRsiValue = getMacdOverRsiValueAtIndex(getLastBeforeLastCloseIndex());
        }
        return crossType == UP
                ? currentMacdOverRsiValue > threshold && prevMacdOverRsiValue <= threshold
                : currentMacdOverRsiValue < threshold && prevMacdOverRsiValue >= threshold;
    }

    private boolean rsiCrossed(CandleType candleType, CrossType crossType, double threshold) {
        double rsiValueNow, rsiValuePrev;
        if (candleType == OPEN) {
            rsiValueNow = getRsiOpenValue();
            rsiValuePrev = getRsiCloseValue();
        } else {
            rsiValueNow = getRsiCloseValue();
            rsiValuePrev = getRSIValueAtIndex(getLastBeforeLastCloseIndex());
        }
        return crossType == UP
                ? rsiValueNow > threshold && rsiValuePrev <= threshold
                : rsiValueNow < threshold && rsiValuePrev >= threshold;
    }

    public double getRsiOpenValue() {
        return rsiIndicator.getValue(endIndex).doubleValue();
    }

    public double getRsiCloseValue() {
        return rsiIndicator.getValue(getLastCloseIndex()).doubleValue();
    }

    public double getRSIValueAtIndex(int index) {
        return rsiIndicator.getValue(index).doubleValue();
    }

    public synchronized Double getCurrentPrice() {
        return currentPrice;
    }

    public double getMacdOverRsiCloseValue() {
        return macdOverRsiCloseValue;
    }

    public int getLastIndex() {
        return endIndex;
    }

    public int getLastCloseIndex() {
        return endIndex - 1;
    }

    public int getLastBeforeLastCloseIndex() {
        return endIndex - 2;
    }

    public enum CandleType {
        OPEN, CLOSE
    }

    public enum CrossType {
        UP, DOWN
    }

    public enum IndicatorType {
        RSI, MACD_OVER_RSI, SMA
    }
}
