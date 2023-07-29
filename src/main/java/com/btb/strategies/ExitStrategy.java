package com.btb.strategies;

import com.btb.data.RealTimeData;
import com.btb.positions.SellingInstructions;

public interface ExitStrategy {
    SellingInstructions run(RealTimeData realTimeData);
}
