package com.btb.strategies;

/**
 * Created by Anton Dyakov on 16.07.2023
 */
public enum EntryStrategyType {
    RSI("rsi"),
    MACD("macd");

    private final String name;

    EntryStrategyType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
