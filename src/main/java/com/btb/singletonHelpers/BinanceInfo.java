package com.btb.singletonHelpers;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.ExchangeInformation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BinanceInfo {
    private static ExchangeInformation exchangeInformation;
    private static Map<String, ExchangeInfoEntry> symbolInformation;

    private static class BinanceInfoHolder {
        private static final BinanceInfo binanceInfo = new BinanceInfo();
    }

    private BinanceInfo() {
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        exchangeInformation = syncRequestClient.getExchangeInformation();
        symbolInformation = new HashMap<>();
        for (ExchangeInfoEntry exchangeInfoEntry : exchangeInformation.getSymbols()) {
            symbolInformation.put(exchangeInfoEntry.getSymbol().toLowerCase(), exchangeInfoEntry);
        }
    }

    public static BinanceInfo getBinanceInfo() {
        return BinanceInfo.BinanceInfoHolder.binanceInfo;
    }

    public static ExchangeInformation getExchangeInformation() {
        return exchangeInformation;
    }

    /**
     * @param symbol need to be upper case.
     * @return the ExchangeInfoEntry of symbol.
     */
    public static ExchangeInfoEntry getSymbolInformation(String symbol) {
        return symbolInformation.get(symbol);
    }

    public static boolean isSymbolExists(String symbol) {
        return symbolInformation.containsKey(symbol);
    }

    public static String formatQty(double buyingQty, String symbol) {
        String formatter = "%." + symbolInformation.get(symbol).getQuantityPrecision().toString() + "f";
        return String.format(Locale.US, formatter, Math.abs(buyingQty));
    }

    public static String formatPrice(double price, String symbol) {
        return String.format("%." + symbolInformation.get(symbol).getPricePrecision().toString() + "f", price);
    }

}
