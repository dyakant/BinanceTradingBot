package com.btb.strategies;

import com.btb.data.RealTimeData;
import com.btb.positions.PositionHandler;

public interface EntryStrategy {
    /**
     * @return Position entry if purchased coins else null.
     */
    PositionHandler run(RealTimeData realTimeData);

    void setTakeProfitPercentage(double takeProfitPercentage);

    void setStopLossPercentage(double stopLossPercentage);

    void setRequestedBuyingAmount(double requestedBuyingAmount);

    String getName();
}
