package com.btb.strategies.macdOverRSIStrategies;

import com.binance.client.model.enums.PositionSide;
import com.binance.client.model.trade.Order;
import com.btb.data.AccountBalance;
import com.btb.data.RealTimeData;
import com.btb.positions.PositionHandler;
import com.btb.singletonHelpers.RequestClient;
import com.btb.singletonHelpers.TelegramMessenger;
import com.btb.strategies.EntryStrategy;
import com.btb.strategies.ExitStrategy;
import com.btb.strategies.macdOverRSIStrategies.Long.*;
import com.btb.strategies.macdOverRSIStrategies.Short.*;
import com.btb.data.Trailer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;

import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderStatus.FILLED;
import static com.binance.client.model.enums.OrderStatus.NEW;
import static com.binance.client.model.enums.PositionSide.LONG;
import static com.binance.client.model.enums.PositionSide.SHORT;
import static com.btb.data.Config.DOUBLE_ZERO;
import static com.btb.data.Config.ZERO;
import static com.btb.data.RealTimeData.CandleType.CLOSE;
import static com.btb.data.RealTimeData.CrossType.DOWN;
import static com.btb.data.RealTimeData.CrossType.UP;
import static com.btb.strategies.EntryStrategyType.MACD;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIConstants.*;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.NEGATIVE;
import static com.btb.strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.POSITIVE;
import static com.btb.utils.Utils.getBuyingQtyAsString;
import static com.btb.utils.Utils.getTime;

@Slf4j
public class MACDOverRSIEntryStrategy implements EntryStrategy {
    public final String name = MACD.getName();
    public final String symbol;
    private final RequestClient requestClient;
    private final AccountBalance accountBalance;
    double takeProfitPercentage = TAKE_PROFIT_PERCENTAGE;
    private double stopLossPercentage = STOP_LOSS_PERCENTAGE;
    private final int leverage = LEVERAGE;
    private double requestedBuyingAmount = BUYING_AMOUNT;
    private volatile boolean bought = false;

    public MACDOverRSIEntryStrategy(String symbol) {
        accountBalance = AccountBalance.getAccountBalance();
        requestClient = RequestClient.getRequestClient();
        requestClient.changeInitialLeverage(symbol, leverage);
        this.symbol = symbol;
    }

    @Override
    public synchronized PositionHandler run(RealTimeData realTimeData) {
        boolean notInPosition = accountBalance.getPosition(symbol).getPositionAmt().compareTo(BigDecimal.valueOf(DOUBLE_ZERO)) == ZERO;
        if (notInPosition) {
            boolean noOpenOrders = requestClient.getOpenOrders(symbol).size() == ZERO;
            if (noOpenOrders) {
                return processDataWithRulesAndMakeOrder(realTimeData);
            }
        }
        return null;
    }

    /**
     * Правило 0: Цена выше SMA - лонг, ниже - шорт
     * Правило 1: Если прошлая свеча по MACD пересекла RSI вверх - лонг
     * Правило 2: Если MACD-RSI ниже 0 и MACD растёт - лонг
     * Правило 3: Если прошлая свеча по MACD пересекла RSI вниз - шорт
     * Правило 4: Если MACD-RSI выше 0 и MACD падает - шорт
     *
     * @param realTimeData - свежие данные
     * @return - набор правил для закрытия позиции
     */
    private PositionHandler processDataWithRulesAndMakeOrder(RealTimeData realTimeData) {
        double currentPrice = realTimeData.getCurrentPrice();
        boolean isCurrentPriceAboveSMA = currentPrice > realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex());
//        logMacdOverRsiValues(realTimeData);
        if (isCurrentPriceAboveSMA) {
            boolean isPreviousMacdCandleCrossedRsiUp = realTimeData.macdOverRsiCrossed(CLOSE, UP, ZERO);
            if (isPreviousMacdCandleCrossedRsiUp) {
                if (bought) return null;
                log.info("{} BUY LONG first branch", symbol);
                return buyAndCreatePositionHandler(currentPrice, LONG);
            } else {
                boolean macdValueBelowZero = realTimeData.getMACDOverRsiValueAtIndex(realTimeData.getLastIndex()) < ZERO;
                boolean isMacdOverRsiGrows = decliningPyramid(realTimeData, NEGATIVE);
                if (macdValueBelowZero && isMacdOverRsiGrows) {
                    if (bought) return null;
                    log.info("{} BUY LONG second branch", symbol);
                    return buyAndCreatePositionHandler(currentPrice, LONG);
                }
            }
            bought = false;
        } else {
            boolean isPreviousMacdCandleCrossedRsiDown = realTimeData.macdOverRsiCrossed(CLOSE, DOWN, ZERO);
            if (isPreviousMacdCandleCrossedRsiDown) {
                if (bought) return null;
                log.info("{} BUY SHORT! first branch", symbol);
                return buyAndCreatePositionHandler(currentPrice, SHORT);
            } else {
                boolean macdValueAboveZero = realTimeData.getMACDOverRsiValueAtIndex(realTimeData.getLastIndex()) > ZERO;
                boolean isMacdOverRsiFall = decliningPyramid(realTimeData, POSITIVE);
                if (macdValueAboveZero && isMacdOverRsiFall) {
                    if (bought) return null;
                    log.info("{} BUY SHORT! second branch", symbol);
                    return buyAndCreatePositionHandler(currentPrice, SHORT);
                }
            }
            bought = false;
        }
        log.trace("{} MACDOverRSIEntryStrategy, no signal to open position", symbol);
        return null;
    }

//    private void logMacdOverRsiValues(DataHolder realTimeData) {
//        if (log.isTraceEnabled()) {
//            int last = realTimeData.getLastIndex(),
//                    first = realTimeData.getLastIndex() - 1,
//                    second = realTimeData.getLastIndex() - 2,
//                    third = realTimeData.getLastIndex() - 3;
//            log.trace("""
//                            {} MACDOverRSIEntryStrategy,\s
//                            [{}]:MacdLineValue={}, SignalLineValue={},
//                            [{}]:MacdLineValue={}, SignalLineValue={},
//                            [{}]:MacdLineValue={}, SignalLineValue={},
//                            [{}]:MacdLineValue={}, SignalLineValue={}""", symbol,
//                    last, realTimeData.getMacdOverRsiMacdLineValueAtIndex(last), realTimeData.getMacdOverRsiSignalLineValueAtIndex(last),
//                    first, realTimeData.getMacdOverRsiMacdLineValueAtIndex(first), realTimeData.getMacdOverRsiSignalLineValueAtIndex(first),
//                    second, realTimeData.getMacdOverRsiMacdLineValueAtIndex(second), realTimeData.getMacdOverRsiSignalLineValueAtIndex(second),
//                    third, realTimeData.getMacdOverRsiMacdLineValueAtIndex(third), realTimeData.getMacdOverRsiSignalLineValueAtIndex(third));
//        }
//    }

    private PositionHandler buyAndCreatePositionHandler(Double currentPrice, PositionSide positionSide) {
        bought = true;
        PositionHandler positionHandler = null;
        try {
            Order order;
            ArrayList<ExitStrategy> exitStrategies;
            String buyingQty = getBuyingQtyAsString(currentPrice, symbol, leverage, requestedBuyingAmount);
            log.info("{} {}, buyingQty={}, currentPrice={}", symbol, positionSide.toString().toLowerCase(), buyingQty, currentPrice);
            if (positionSide == LONG) {
                order = requestClient.postLimitOrder(symbol, BUY, buyingQty, currentPrice.toString());
                exitStrategies = defineLongExitStrategy(currentPrice);
            } else {
                order = requestClient.postLimitOrder(symbol, SELL, buyingQty, currentPrice.toString());
                exitStrategies = defineShortExitStrategy(currentPrice);
            }
            positionHandler = new PositionHandler(order, exitStrategies);
            log.info("{}, buyOrder: {}", symbol, order);
            sendTelegramMessageAboutOrder(positionSide.toString().toLowerCase(), order);
        } catch (Exception e) {
            log.error("buyAndCreatePositionHandler exception:", e);
        }
        return positionHandler;
    }

    private void sendTelegramMessageAboutOrder(String positionSide, Order order) {
        String message;
        if (NEW.toString().equals(order.getStatus())) {
            message = String.format("%s order to %s %s by %s was placed at %s",
                    positionSide, order.getSide().toLowerCase(),
                    order.getOrigQty(), order.getPrice(), getTime(order.getUpdateTime()));
        } else if (FILLED.toString().equals(order.getStatus())) {
            message = String.format("%s order to %s %s by %s was executed at %s",
                    positionSide, order.getSide().toLowerCase(),
                    order.getOrigQty(), order.getPrice(), getTime(order.getUpdateTime()));
        } else {
            message = String.format("%s order to %s %s by %s was executed at %s, status %s",
                    positionSide, order.getSide().toLowerCase(),
                    order.getOrigQty(), order.getPrice(), getTime(order.getUpdateTime()), order.getStatus());
        }
        TelegramMessenger.send(symbol, message);
    }

    @NotNull
    private ArrayList<ExitStrategy> defineLongExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSILongExitStrategy1());
        exitStrategies.add(new MACDOverRSILongExitStrategy2());
        exitStrategies.add(new MACDOverRSILongExitStrategy3(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy4(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy5(new Trailer(currentPrice, CONSTANT_TRAILING_PERCENTAGE, LONG)));
        return exitStrategies;
    }

    @NotNull
    private ArrayList<ExitStrategy> defineShortExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSIShortExitStrategy1());
        exitStrategies.add(new MACDOverRSIShortExitStrategy2());
        exitStrategies.add(new MACDOverRSIShortExitStrategy3(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy4(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy5(new Trailer(currentPrice, CONSTANT_TRAILING_PERCENTAGE, SHORT)));
        return exitStrategies;
    }

    @Override
    public void setTakeProfitPercentage(double takeProfitPercentage) {
        this.takeProfitPercentage = takeProfitPercentage;
    }

    @Override
    public void setStopLossPercentage(double stopLossPercentage) {
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    public void setRequestedBuyingAmount(double requestedBuyingAmount) {
        this.requestedBuyingAmount = requestedBuyingAmount;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Сравнивает три точки, текущую, предыдущую и перед предыдущей
     *
     * @param realTimeData - реал-тайм данные
     * @param type         - фаза
     * @return - true для NEGATIVE при росте MACD и для POSITIVE при снижении MACD
     */
    private boolean decliningPyramid(RealTimeData realTimeData, DecliningType type) {
        boolean rule1;
        boolean rule2;
        double currentMacdOverRsiValue = realTimeData.getMACDOverRsiValueAtIndex(realTimeData.getLastIndex() - 1);
        double prevMacdOverRsiValue = realTimeData.getMACDOverRsiValueAtIndex(realTimeData.getLastIndex() - 2);
        double prevPrevMacdOverRsiValue = realTimeData.getMACDOverRsiValueAtIndex(realTimeData.getLastIndex() - 3);
        if (type == NEGATIVE) {
            rule1 = currentMacdOverRsiValue > prevMacdOverRsiValue;
            rule2 = prevMacdOverRsiValue > prevPrevMacdOverRsiValue;
        } else {
            rule1 = currentMacdOverRsiValue < prevMacdOverRsiValue;
            rule2 = prevMacdOverRsiValue < prevPrevMacdOverRsiValue;
        }
        return rule1 && rule2;
    }

    public enum DecliningType {
        NEGATIVE,
        POSITIVE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MACDOverRSIEntryStrategy that = (MACDOverRSIEntryStrategy) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
