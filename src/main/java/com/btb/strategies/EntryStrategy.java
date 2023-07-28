package com.btb.strategies;

import com.btb.data.DataHolder;
import com.btb.positions.PositionHandler;

public interface EntryStrategy {
    /**
     * @return Position entry if purchased coins else null.
     */
    PositionHandler run(DataHolder realTimeData);

    void setTakeProfitPercentage(double takeProfitPercentage);

    void setStopLossPercentage(double stopLossPercentage);

    void setRequestedBuyingAmount(double requestedBuyingAmount);

    String getName();
}
