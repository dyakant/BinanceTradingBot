package com.btb.strategies.macdOverRSIStrategies;

import com.btb.data.DataHolder;
import com.btb.strategies.ExitStrategy;

public abstract class MACDOverRSIBaseExitStrategy implements ExitStrategy {

    public boolean isDirectionChangedAndPositiveThreeHistogram(DataHolder realTimeData) {
        return isCurrentAbsMACDLessPrevious(realTimeData) && isLastThreeMACDCandlesAboveZro(realTimeData);
    }

    public boolean isDirectionChangedAndNegativeThreeHistogram(DataHolder realTimeData) {
        return isCurrentAbsMACDLessPrevious(realTimeData) && isLastThreeMACDCandlesBellowZero(realTimeData);
    }

    public boolean isPositiveHistogram(DataHolder data) {
        return isCurrentAbsMACDGreaterOrEqualPrevious(data) && isLastThreeMACDCandlesAboveZro(data);
    }

    public boolean isNegativeHistogram(DataHolder data) {
        return isCurrentAbsMACDGreaterOrEqualPrevious(data) && isLastThreeMACDCandlesBellowZero(data);
    }

    private boolean isCurrentAbsMACDGreaterOrEqualPrevious(DataHolder realTimeData) {
        return Math.abs(getPrevFirst(realTimeData)) >= Math.abs(getPrevSecond(realTimeData));
    }

    private boolean isCurrentAbsMACDLessPrevious(DataHolder realTimeData) {
        return Math.abs(getPrevFirst(realTimeData)) < Math.abs(getPrevSecond(realTimeData));
    }

    private boolean isLastThreeMACDCandlesAboveZro(DataHolder data) {
        return getPrevFirst(data) >= 0 && getPrevSecond(data) >= 0 && getPrevThird(data) >= 0;
    }

    private boolean isLastThreeMACDCandlesBellowZero(DataHolder data) {
        return getPrevFirst(data) < 0 && getPrevSecond(data) < 0 && getPrevThird(data) < 0;
    }

    private double getPrevFirst(DataHolder data) {
        return data.getMacdOverRsiValueAtIndex(data.getLastIndex() - 1);
    }

    private double getPrevSecond(DataHolder data) {
        return data.getMacdOverRsiValueAtIndex(data.getLastIndex() - 2);
    }

    private double getPrevThird(DataHolder data) {
        return data.getMacdOverRsiValueAtIndex(data.getLastIndex() - 3);
    }
}
