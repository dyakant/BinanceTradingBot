package strategies.macdOverRSIStrategies;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.PositionSide;
import com.binance.client.model.trade.Order;
import data.AccountBalance;
import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import positions.PositionHandler;
import singletonHelpers.RequestClient;
import singletonHelpers.TelegramMessenger;
import strategies.EntryStrategy;
import strategies.ExitStrategy;
import strategies.macdOverRSIStrategies.Long.*;
import strategies.macdOverRSIStrategies.Short.*;
import utils.Trailer;

import java.math.BigDecimal;
import java.util.ArrayList;

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.PositionSide.LONG;
import static com.binance.client.model.enums.PositionSide.SHORT;
import static com.binance.client.model.enums.TimeInForce.GTC;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static data.Config.DOUBLE_ZERO;
import static data.Config.ZERO;
import static data.DataHolder.CandleType.CLOSE;
import static data.DataHolder.CrossType.DOWN;
import static data.DataHolder.CrossType.UP;
import static data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.*;
import static strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.NEGATIVE;
import static strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.POSITIVE;

@Slf4j
public class MACDOverRSIEntryStrategy implements EntryStrategy {
    public static final String NAME = "macd";
    private final SyncRequestClient syncRequestClient;
    private final AccountBalance accountBalance;
    double takeProfitPercentage = TAKE_PROFIT_PERCENTAGE;
    private double stopLossPercentage = STOP_LOSS_PERCENTAGE;
    private final int leverage = LEVERAGE;
    private double requestedBuyingAmount = BUYING_AMOUNT;
    private volatile boolean bought = false;

    public MACDOverRSIEntryStrategy(String symbol) {
        accountBalance = AccountBalance.getAccountBalance();
        syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        syncRequestClient.changeInitialLeverage(symbol, leverage);
    }

    @Override
    public synchronized PositionHandler run(DataHolder realTimeData, String symbol) {
        boolean notInPosition = accountBalance.getPosition(symbol).getPositionAmt().compareTo(BigDecimal.valueOf(DOUBLE_ZERO)) == ZERO;
        if (notInPosition) {
            boolean noOpenOrders = syncRequestClient.getOpenOrders(symbol).size() == ZERO;
            if (noOpenOrders) {
                return processDataWithRulesAndMakeOrder(realTimeData, symbol);
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
     * @param symbol       - тикер
     * @return - набор правил для закрытия позиции
     */
    private PositionHandler processDataWithRulesAndMakeOrder(DataHolder realTimeData, String symbol) {
        double currentPrice = realTimeData.getCurrentPrice();
        boolean isCurrentPriceAboveSMA = currentPrice > realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex());
        if (isCurrentPriceAboveSMA) {
            boolean isPreviousMacdCandleCrossedRsiUp = realTimeData.crossed(MACD_OVER_RSI, CLOSE, UP, ZERO);
            if (isPreviousMacdCandleCrossedRsiUp) {
                if (bought) return null;
                log.info("{} BUY LONG! MACDOverRSIEntryStrategy, isPreviousMacdCandleCrossedRsiUp={}", symbol, isPreviousMacdCandleCrossedRsiUp);
                return buyAndCreatePositionHandler(symbol, currentPrice, LONG);
            } else {
                boolean macdValueBelowZero = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) < ZERO;
                boolean isMacdOverRsiGrows = decliningPyramid(realTimeData, NEGATIVE);
                if (macdValueBelowZero && isMacdOverRsiGrows) {
                    if (bought) return null;
                    log.info("{} BUY LONG! MACDOverRSIEntryStrategy, macdValueBelowZero={}, isMacdOverRsiGrows={}", symbol, realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()), isMacdOverRsiGrows);
                    return buyAndCreatePositionHandler(symbol, currentPrice, LONG);
                }
            }
            bought = false;
        } else {
            boolean isPreviousMacdCandleCrossedRsiDown = realTimeData.crossed(MACD_OVER_RSI, CLOSE, DOWN, ZERO);
            if (isPreviousMacdCandleCrossedRsiDown) {
                if (bought) return null;
                log.info("{} BUY SHORT! MACDOverRSIEntryStrategy, isPreviousMacdCandleCrossedRsiDown={}", symbol, isPreviousMacdCandleCrossedRsiDown);
                return buyAndCreatePositionHandler(symbol, currentPrice, SHORT);
            } else {
                boolean macdValueAboveZero = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) > ZERO;
                boolean isMacdOverRsiFall = decliningPyramid(realTimeData, POSITIVE);
                if (macdValueAboveZero && isMacdOverRsiFall) {
                    if (bought) return null;
                    log.info("{} BUY SHORT! MACDOverRSIEntryStrategy, macdValueAboveZero={}, isMacdOverRsiFall={}", symbol, realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()), isMacdOverRsiFall);
                    return buyAndCreatePositionHandler(symbol, currentPrice, SHORT);
                }
            }
            bought = false;
        }
        return null;
    }

    private PositionHandler buyAndCreatePositionHandler(String symbol, Double currentPrice, PositionSide positionSide) {
        bought = true;
        PositionHandler positionHandler = null;
        try {
            Order buyOrder;
            ArrayList<ExitStrategy> exitStrategies;
            String buyingQty = utils.Utils.getBuyingQtyAsString(currentPrice, symbol, leverage, requestedBuyingAmount);
            TelegramMessenger.send(symbol, "buying " + positionSide.toString().toLowerCase() + ": " + buyingQty + ", price " + currentPrice);
            log.info("{} {}, buyingQty={}, currentPrice={}", symbol, positionSide.toString().toLowerCase(), buyingQty, currentPrice);
            if (positionSide == LONG) {
                buyOrder = postOrder(symbol, BUY, currentPrice.toString(), buyingQty);
                exitStrategies = defineLongExitStrategy(currentPrice);
            } else {
                buyOrder = postOrder(symbol, SELL, currentPrice.toString(), buyingQty);
                exitStrategies = defineShortExitStrategy(currentPrice);
            }
            positionHandler = new PositionHandler(buyOrder, exitStrategies);
            log.info("{}, buyOrder: {}", symbol, buyOrder);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return positionHandler;
    }

    private Order postOrder(String symbol, OrderSide orderSide, String currentPrice, String buyingQty) {
        return syncRequestClient.postOrder(
                symbol,
                orderSide,
                null,
                LIMIT,
                GTC,
                buyingQty,
                currentPrice,
                null,
                null,
                null,
                null,
                null,
                null,
                MARK_PRICE,
                null,
                RESULT);
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
        return NAME;
    }

    /**
     * Сравнивает три точки, текущую, предыдущую и перед предыдущей
     *
     * @param realTimeData - реал-тайм данные
     * @param type         - фаза
     * @return - true для NEGATIVE при росте MACD и для POSITIVE при снижении MACD
     */
    private boolean decliningPyramid(DataHolder realTimeData, DecliningType type) {
        boolean rule1;
        boolean rule2;
        double currentMacdOverRsiValue = realTimeData.getMacdOverRsiCloseValue();
        double prevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 2);
        double prevPrevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 3);
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
}
