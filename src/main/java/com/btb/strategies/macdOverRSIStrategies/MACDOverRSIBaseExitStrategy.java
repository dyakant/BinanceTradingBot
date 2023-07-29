package com.btb.strategies.macdOverRSIStrategies;

import com.btb.data.RealTimeData;
import com.btb.strategies.ExitStrategy;

public abstract class MACDOverRSIBaseExitStrategy implements ExitStrategy {

    public boolean isDirectionChangedAndPositiveThreeHistogram(RealTimeData realTimeData) {
        return isCurrentAbsMACDLessPrevious(realTimeData) && isLastThreeMACDCandlesAboveZro(realTimeData);
    }

    public boolean isDirectionChangedAndNegativeThreeHistogram(RealTimeData realTimeData) {
        return isCurrentAbsMACDLessPrevious(realTimeData) && isLastThreeMACDCandlesBellowZero(realTimeData);
    }

    public boolean isPositiveHistogram(RealTimeData data) {
        return isCurrentAbsMACDGreaterOrEqualPrevious(data) && isLastThreeMACDCandlesAboveZro(data);
    }

    public boolean isNegativeHistogram(RealTimeData data) {
        return isCurrentAbsMACDGreaterOrEqualPrevious(data) && isLastThreeMACDCandlesBellowZero(data);
    }

    private boolean isCurrentAbsMACDGreaterOrEqualPrevious(RealTimeData realTimeData) {
        return Math.abs(getPrevFirst(realTimeData)) >= Math.abs(getPrevSecond(realTimeData));
    }

    private boolean isCurrentAbsMACDLessPrevious(RealTimeData realTimeData) {
        return Math.abs(getPrevFirst(realTimeData)) < Math.abs(getPrevSecond(realTimeData));
    }

    private boolean isLastThreeMACDCandlesAboveZro(RealTimeData data) {
        return getPrevFirst(data) >= 0 && getPrevSecond(data) >= 0 && getPrevThird(data) >= 0;
    }

    private boolean isLastThreeMACDCandlesBellowZero(RealTimeData data) {
        return getPrevFirst(data) < 0 && getPrevSecond(data) < 0 && getPrevThird(data) < 0;
    }

    private double getPrevFirst(RealTimeData data) {
        return data.getMACDOverRsiValueAtIndex(data.getLastIndex() - 1);
    }

    private double getPrevSecond(RealTimeData data) {
        return data.getMACDOverRsiValueAtIndex(data.getLastIndex() - 2);
    }

    private double getPrevThird(RealTimeData data) {
        return data.getMACDOverRsiValueAtIndex(data.getLastIndex() - 3);
    }
}
