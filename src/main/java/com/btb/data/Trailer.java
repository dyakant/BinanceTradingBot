package com.btb.data;

import com.binance.client.model.enums.PositionSide;
import lombok.extern.slf4j.Slf4j;

import static com.binance.client.model.enums.PositionSide.LONG;

/**
 * The combination of updateExitPrice() and needToSell() moves the exit price
 * depending on trailingPercentage
 */
@Slf4j
public class Trailer {
    private final PositionSide side;
    private final Double trailingPercentage;
    private double absoluteMaxPrice;
    private double exitPrice;

    public Trailer(double currentPrice, Double trailingPercentage, PositionSide side) {
        this.absoluteMaxPrice = currentPrice;
        this.side = side;
        this.trailingPercentage = trailingPercentage;
        if (side == LONG) {
            exitPrice = calculateLongTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
        } else {
            exitPrice = calculateShortTrailingExitPrices(absoluteMaxPrice, trailingPercentage);
        }
    }

    public void updateExitPrice(double currentPrice) {
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
        log.trace("updateTrailer, side={}, currentPrice={}, absoluteMaxPrice={}, exitPrice={}", side, currentPrice, absoluteMaxPrice, exitPrice);
    }

    /**
     * if the current price crosses the exitPrice
     * then it's time to close a position
     * @param currentPrice current price
     * @return boolean
     */
    public boolean needToSell(double currentPrice) {
        return (side == LONG)
                ? currentPrice <= exitPrice
                : currentPrice >= exitPrice;
    }

    private double calculateShortTrailingExitPrices(double highestPrice, Double trailingPercentage) {
        return highestPrice + (highestPrice * trailingPercentage / 100);
    }

    private double calculateLongTrailingExitPrices(double highestPrice, Double trailingPercentage) {
        return highestPrice - (highestPrice * trailingPercentage / 100);
    }

    public void setAbsoluteMaxPrice(double absoluteMaxPrice) {
        this.absoluteMaxPrice = absoluteMaxPrice;
    }
}
