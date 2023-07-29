package com.btb.utils;

import com.binance.client.model.enums.CandlestickInterval;
import com.btb.data.RealTimeData;
import com.btb.singletonHelpers.BinanceInfo;

import java.time.*;

import static com.btb.utils.TimeConstants.*;

public class Utils {
    public static Long candleStickIntervalToMilliseconds(CandlestickInterval interval) {
        String intervalCode = interval.toString();
        int value = Integer.parseInt(intervalCode.substring(0, intervalCode.length() - 1));
        char typeOfTime = intervalCode.charAt(intervalCode.length() - 1);
        return switch (typeOfTime) {
            case 'm' -> (long) value * MINUTES_TO_MILLISECONDS_CONVERTER;
            case 'h' -> (long) value * HOURS_TO_MILLISECONDS_CONVERTER;
            case 'd' -> (long) value * DAYS_TO_MILLISECONDS_CONVERTER;
            case 'w' -> (long) value * WEEKS_TO_MILLISECONDS_CONVERTER;
            case 'M' -> (long) value * MONTHS_TO_MILLISECONDS_CONVERTER;
            default -> -1L;
        };
    }

    public static ZonedDateTime getZonedDateTime(Long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    public static String getTime(Long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalTime localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        return String.format("%02d:%02d:%02d.%d",
                localTime.getHour(),
                localTime.getMinute(),
                localTime.getSecond(),
                (timestamp % 1000));
    }

    public static String getBuyingQtyAsString(double currentPrice, String symbol, int leverage, double requestedBuyingAmount) {
        double buyingQty = requestedBuyingAmount * leverage / currentPrice;
        return fixQuantity(BinanceInfo.formatQty(buyingQty, symbol));
    }

    public static String getTakeProfitPriceAsString(RealTimeData data, String symbol, double takeProfitPercentage) {
        double takeProfitPrice = data.getCurrentPrice() + data.getCurrentPrice() * takeProfitPercentage;
        return BinanceInfo.formatPrice(takeProfitPrice, symbol);
    }

    public static String getStopLossPriceAsString(RealTimeData data, String symbol, double stopLossPercentage) {
        double stopLossPrice = data.getCurrentPrice() - data.getCurrentPrice() * stopLossPercentage;
        return BinanceInfo.formatPrice(stopLossPrice, symbol);
    }

    public static String fixQuantity(String amt) {
        if (Double.parseDouble(amt) == 0) {
            amt = amt.substring(0, amt.length() - 1).concat("1");
        }
        return amt;
    }
}
