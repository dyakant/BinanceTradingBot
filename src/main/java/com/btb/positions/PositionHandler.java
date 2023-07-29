package com.btb.positions;

import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.trade.Order;
import com.btb.data.AccountBalance;
import com.btb.data.RealTimeData;
import com.btb.singletonHelpers.RequestClient;
import com.btb.singletonHelpers.TelegramMessenger;
import com.btb.strategies.ExitStrategy;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.OrderType.MARKET;
import static com.binance.client.model.enums.PositionSide.LONG;
import static com.binance.client.model.enums.PositionSide.SHORT;
import static com.btb.data.Config.*;
import static com.btb.singletonHelpers.BinanceInfo.formatQty;
import static com.btb.utils.Utils.*;

@Slf4j
public class PositionHandler implements Serializable {
    private final RequestClient requestClient;
    //    private final SyncRequestClient syncRequestClient;
    private final AccountBalance accountBalance;
    private String clientOrderId;
    private Long orderID;
    private double qty = 0;
    private final String symbol;
    private volatile boolean isActive = false;
    private String status;
    private final ArrayList<ExitStrategy> exitStrategies;
    private Long baseTime = 0L;
    private volatile boolean rebuying = true;
    private volatile boolean isSelling = false;
    private volatile boolean terminated = false;

    public PositionHandler(Order order, ArrayList<ExitStrategy> _exitStrategies) {
        requestClient = RequestClient.getRequestClient();
        accountBalance = AccountBalance.getAccountBalance();
        clientOrderId = order.getClientOrderId();
        orderID = order.getOrderId();
        symbol = order.getSymbol().toLowerCase();
        exitStrategies = _exitStrategies;
        status = order.getStatus();
    }

    public synchronized boolean isSoldOut() {
        return isActive && isSelling && (!status.equals(NEW)) && (!rebuying) && (qty == 0);
    }

    public synchronized void run(RealTimeData realTimeData) {
        isSelling = false;
        for (ExitStrategy exitStrategy : exitStrategies) {
            SellingInstructions sellingInstructions = exitStrategy.run(realTimeData);
            if ((!isSelling) && sellingInstructions != null) {
                isSelling = true;
                closePosition(sellingInstructions, realTimeData);
                break;
            }
        }
    }

    public synchronized void update(CandlestickInterval interval) {
        rebuying = false;
        try {
            Order order = requestClient.getOrder(symbol, orderID, clientOrderId);
            log.info("{} Update order information, order={}", symbol, order);
            if (NEW.equals(status) && FILLED.equals(order.getStatus())) { // TODO: Place it in isActive()
                String message = String.format("order was executed at %s", getTime(order.getUpdateTime()));
                TelegramMessenger.send(symbol, message);
                log.info("{} status changed from NEW to {}", symbol, order.getStatus());
            }
            status = order.getStatus();
            isActive(order, interval);
            qty = accountBalance.getPosition(symbol).getPositionAmt().doubleValue();
        } catch (Exception e) {
            log.error("Error during update position status", e);
        }
    }

    private void isActive(Order order, CandlestickInterval interval) {
        if (status.equals(NEW)) {
            rebuyOrder(order);
        } else if (status.equals(PARTIALLY_FILLED)) {
            log.info("{} order is partially filled, baseTime={}, order={}", symbol, baseTime, order);
            Long updateTime = order.getUpdateTime();
            if (baseTime.equals(0L)) {
                baseTime = updateTime;
            } else {
                long difference = updateTime - baseTime;
                Long intervalInMilliSeconds = candleStickIntervalToMilliseconds(interval);
                if (difference >= (intervalInMilliSeconds / 2.0)) {
                    cancelOrder();
                    isActive = true;
                }
            }
        } else { // FULL. since in NEW we don't go in the function.
            isActive = true;
        }
    }

    private synchronized void rebuyOrder(Order order) {
        rebuying = true;
        try {
            log.info("{} rebuyOrder, order={}", symbol, order);
            ResponseResult rr = requestClient.cancelAllOpenOrder(symbol);
            log.info("{} All orders were cancelled with result {} {}", symbol, rr.getCode(), rr.getMsg());
            OrderSide side = stringToOrderSide(order.getSide());
            Order buyOrder = requestClient.postOrder(symbol, side, MARKET, order.getOrigQty().toString(), String.valueOf(order.getPrice()));
            log.info("{} rebuying order is placed {}", symbol, buyOrder);
            clientOrderId = buyOrder.getClientOrderId();
            orderID = buyOrder.getOrderId();
            TelegramMessenger.send(symbol, "repeat order " + (BUY.toString().equals(buyOrder.getSide()) ? LONG : SHORT)
                    + ", " + buyOrder.getExecutedQty() + " by avgPrice=" + buyOrder.getActivatePrice() + ", status=" + buyOrder.getStatus());
        } catch (Exception e) {
            log.error("{} Error during rebuying order", symbol, e);
        }
    }

    private synchronized void cancelOrder() {
        try {
            Order order = requestClient.cancelOrder(symbol, orderID, clientOrderId);
            log.info("{} cancel order, order: {}", symbol, order);
            TelegramMessenger.send(symbol, "cancel order, {}" + order);
        } catch (Exception e) {
            log.error("{} Error during canceling order", symbol, e);
        }
    }

    private OrderSide stringToOrderSide(String side) {
        for (OrderSide orderSide : OrderSide.values()) {
            if (orderSide.toString().equals(side)) return orderSide;
        }
        return null;
    }

    private double percentageOfQuantity(double percentage) {
        return (qty * percentage) / 100;
    }

    public void terminate() {
        if (!terminated) {
            terminated = true;
            requestClient.cancelAllOpenOrder(symbol);
            TelegramMessenger.send(symbol, "Position closed!\nBalance:  " + AccountBalance.getBalanceUsdt());
        }
    }

    private void closePosition(SellingInstructions sellingInstructions, RealTimeData realTimeData) {
        String sellingQty = fixQuantity(formatQty(percentageOfQuantity(sellingInstructions.getSellingQtyPercentage()), symbol));
        try {
            Order order = null;
            String currentPrice = realTimeData.getCurrentPrice().toString();
            log.info("{} closePosition with type {}", symbol, sellingInstructions.getType());
            switch (sellingInstructions.getType()) {
                case CLOSE_LONG_LIMIT -> order = requestClient.postOrder(symbol, SELL, LIMIT, sellingQty, currentPrice);
                case CLOSE_LONG_MARKET -> order = requestClient.postOrder(symbol, SELL, MARKET, sellingQty, null);
                case CLOSE_SHORT_LIMIT -> order = requestClient.postOrder(symbol, BUY, LIMIT, sellingQty, currentPrice);
                case CLOSE_SHORT_MARKET -> order = requestClient.postOrder(symbol, BUY, MARKET, sellingQty, null);
                case STAY_IN_POSITION, default -> {
                }
            }
            if (Objects.nonNull(order)) {
                log.info("{} position closed, order: {}", symbol, order);
                TelegramMessenger.send(symbol, "Order to close position was placed. Selling price:  " + currentPrice);
            } else {
                log.info("{} position not closed, order is empty", symbol);
                TelegramMessenger.send(symbol, "Not done. " + sellingInstructions.getType());
            }
        } catch (Exception e) {
            log.error("Exception during closePosition: ", e);
            TelegramMessenger.send(symbol, "Exception happened");
        }
    }

    public enum ClosePositionTypes {
        STAY_IN_POSITION,
        CLOSE_LONG_MARKET,
        CLOSE_LONG_LIMIT,
        CLOSE_SHORT_MARKET,
        CLOSE_SHORT_LIMIT
    }
}
