package com.btb.positions;

public class SellingInstructions {
    private final PositionHandler.ClosePositionTypes type;
    private double sellingQtyPercentage;

    public SellingInstructions(PositionHandler.ClosePositionTypes type, double sellingQtyPercentage) {
        this.type = type;
        this.sellingQtyPercentage = sellingQtyPercentage;
    }

    public PositionHandler.ClosePositionTypes getType() {
        return type;
    }

    public double getSellingQtyPercentage() {
        return sellingQtyPercentage;
    }

    public void setSellingQtyPercentage(double sellingQtyPercentage) {
        this.sellingQtyPercentage = sellingQtyPercentage;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", sellingQtyPercentage=" + sellingQtyPercentage +
                '}';
    }
}
