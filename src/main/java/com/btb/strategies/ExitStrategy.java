package com.btb.strategies;

import com.btb.data.DataHolder;
import com.btb.positions.SellingInstructions;

public interface ExitStrategy {
    SellingInstructions run(DataHolder realTimeData);
}
