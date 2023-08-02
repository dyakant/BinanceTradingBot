package com.btb.positions;

import com.btb.positions.PositionHandler.ClosePositionTypes;

public record SellingInstructions(ClosePositionTypes type, double sellingQtyPercentage) {
}
