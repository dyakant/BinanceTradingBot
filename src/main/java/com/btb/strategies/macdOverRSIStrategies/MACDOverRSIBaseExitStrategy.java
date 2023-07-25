package com.btb.strategies.macdOverRSIStrategies;

import com.btb.data.DataHolder;
import com.btb.strategies.ExitStrategy;

public abstract class MACDOverRSIBaseExitStrategy implements ExitStrategy {

    public boolean changedDirectionAndPositiveThreeHistogram(DataHolder realTimeData) {
        double current = realTimeData.getMacdOverRsiCloseValue();
        double prev = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 1);
        double third = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2);
        boolean isCurrentBelowPrevious = Math.abs(current) <= Math.abs(prev);
        return isCurrentBelowPrevious && current > 0 && prev > 0 && third > 0;
    }

    public boolean changedDirectionAndNegativeThreeHistogram(DataHolder realTimeData) {
        double current = realTimeData.getMacdOverRsiCloseValue();
        double prev = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 1);
        double third = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2);
        boolean isCurrentBelowPrevious = Math.abs(current) <= Math.abs(prev);
        return isCurrentBelowPrevious && current < 0 && prev < 0 && third < 0;
    }

    public boolean stayInTrackAndThreePositiveHistograms(DataHolder realTimeData) {
        double current = realTimeData.getMacdOverRsiCloseValue();
        double prev = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 1);
        double third = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2);
        boolean isCurrentAbovePrevious = Math.abs(current) >= Math.abs(prev); // TODO: Is it correct?
        return isCurrentAbovePrevious && current > 0 && prev > 0 && third > 0;
    }

    public boolean stayInTrackAndThreeNegativeHistograms(DataHolder realTimeData) {
        double current = realTimeData.getMacdOverRsiCloseValue();
        double prev = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 1);
        double third = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastCloseIndex() - 2);
        boolean isCurrentAbovePrevious = Math.abs(current) >= Math.abs(prev); // TODO: Is it correct?
        return isCurrentAbovePrevious && current < 0 && prev < 0 && third < 0;
    }
}
