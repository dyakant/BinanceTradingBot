package positions;

public class SellingInstructions {
    private final String strategyName;
    private final PositionHandler.ClosePositionTypes type;
    private double sellingQtyPercentage;

    public SellingInstructions(PositionHandler.ClosePositionTypes type, double sellingQtyPercentage, String strategyName) {
        this.type = type;
        this.sellingQtyPercentage = sellingQtyPercentage;
        this.strategyName = strategyName;
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
                "strategyName='" + strategyName + '\'' +
                ", type=" + type +
                ", sellingQtyPercentage=" + sellingQtyPercentage +
                '}';
    }
}
