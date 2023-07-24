package utils;

import com.binance.client.model.enums.PositionSide;
import lombok.extern.slf4j.Slf4j;

import static com.binance.client.model.enums.PositionSide.LONG;

@Slf4j
public class Trailer {
    private double absoluteMaxPrice;
    private double exitPrice;
    private PositionSide side;
    private final Double trailingPercentage;

    public Trailer(double currentPrice, Double trailingPercentage, PositionSide side) {
        absoluteMaxPrice = currentPrice;
        this.side = side;
        this.trailingPercentage = trailingPercentage;
        if (side == LONG) {
            exitPrice = calculateLongTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
        } else {
            exitPrice = calculateShortTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
        }
    }

    public void updateTrailer(double currentPrice) {
        double prevAbsoluteMaxPrice = absoluteMaxPrice;
        if (side == LONG) {
            if (currentPrice > absoluteMaxPrice) {
                absoluteMaxPrice = currentPrice;
                exitPrice = calculateLongTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
            }
        } else {
            if (currentPrice < absoluteMaxPrice) {
                absoluteMaxPrice = currentPrice;
                exitPrice = calculateShortTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
            }
        }
        log.info("updateTrailer, side={}, prevAbsoluteMaxPrice={}, currentPrice={}, absoluteMaxPrice={}, exitPrice={}", side, prevAbsoluteMaxPrice, currentPrice, absoluteMaxPrice, exitPrice);
    }

    public boolean needToSell(double currentPrice) {
        if (side == LONG) return currentPrice <= exitPrice;
        else return currentPrice >= exitPrice;
    }

    private double calculateShortTrailingExitPrices(double highestPrice, Double trailingPercentage) {
        return highestPrice + (highestPrice * trailingPercentage / 100);
    }

    private double calculateLongTrailingExitPrices(double highestPrice, Double trailingPercentage) {
        return highestPrice - (highestPrice * trailingPercentage / 100);
    }

    public double getAbsoluteMaxPrice() {
        return absoluteMaxPrice;
    }

    public void setAbsoluteMaxPrice(double absoluteMaxPrice) {
        this.absoluteMaxPrice = absoluteMaxPrice;
    }

    public double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public PositionSide getSide() {
        return side;
    }

    public void setSide(PositionSide side) {
        this.side = side;
    }

    public Double getTrailingPercentage() {
        return trailingPercentage;
    }

}
