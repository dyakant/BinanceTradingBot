package com.btb.positions;

import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderStatus;
import com.binance.client.model.trade.Order;
import com.btb.data.AccountBalance;
import com.btb.data.RealTimeData;
import com.btb.singletonHelpers.TelegramMessenger;
import com.btb.strategies.ExitStrategy;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;

import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderStatus.*;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.OrderType.MARKET;
import static com.binance.client.model.enums.PositionSide.LONG;
import static com.binance.client.model.enums.PositionSide.SHORT;
import static com.btb.data.AccountBalance.getAccountBalance;
import static com.btb.singletonHelpers.BinanceInfo.formatQty;
import static com.btb.singletonHelpers.RequestClient.getRequestClient;
import static com.btb.utils.Utils.*;

@Slf4j
public class PositionHandler implements Serializable {
    private final String symbol;
    private final ArrayList<ExitStrategy> exitStrategies;
    private String clientOrderId;
    private Long orderID;
    private double qty = 0;
    private OrderStatus status;
    private Long baseTime = 0L;
//    private volatile boolean isActive = false;
//    private volatile boolean isSelling = false;
    private volatile boolean terminated = false;

    public PositionHandler(Order order, ArrayList<ExitStrategy> _exitStrategies) {
        symbol = order.getSymbol().toLowerCase();
        exitStrategies = _exitStrategies;
        updatePositionInfo(order);
    }

    public synchronized void update(CandlestickInterval interval) {
        try {
            qty = getAccountBalance().getPosition(symbol).getPositionAmt().doubleValue();
            Order order = getRequestClient().getOrder(symbol, orderID, clientOrderId);
            processOrderStatus(order, interval);
            log.info("{} Update order information, qty={}, order={}", symbol, qty, order);
        } catch (Exception e) {
            log.error("Error during update position status", e);
        }
    }

    public synchronized boolean shouldStopTrading() {
        return qty == 0 && !status.equals(NEW); // && isActive && isSelling;
    }

    public void stopTrading() {
        if (!terminated) {
            terminated = true;
            ResponseResult res = getRequestClient().cancelAllOpenOrder(symbol);
            if (res.getCode() == 200) {
                TelegramMessenger.send(symbol, "Trading closed.\nBalance:  " + AccountBalance.getBalanceUsdt());
            } else {
                TelegramMessenger.send(symbol, "Errors during terminating trading, errCode:" + res.getCode() + ", errMsg:" + res.getMsg());
            }
        }
    }

    public synchronized void process(RealTimeData realTimeData) {
//        isSelling = false;
        for (ExitStrategy exitStrategy : exitStrategies) {
            SellingInstructions sellingInstructions = exitStrategy.run(realTimeData);
//            if ((!isSelling) && sellingInstructions != null) {
            if (sellingInstructions != null) {
//                isSelling = true;
                closePosition(sellingInstructions, realTimeData);
                break;
            }
        }
    }

    private void updatePositionInfo(Order order) {
        clientOrderId = order.getClientOrderId();
        orderID = order.getOrderId();
        status = OrderStatus.valueOf(order.getStatus());
        sendTelegramMessageAboutOrder(order);
        log.debug("{} position updated with new order, clientOrderId={}, orderID={}, status={}\norder:{}",
                symbol, clientOrderId, orderID, status, order);
    }

    private void sendTelegramMessageAboutOrder(Order order) {
        String message;
        if (NEW.toString().equals(order.getStatus())) {
            message = String.format("Order to %s %s by %s was placed at %s",
                    order.getSide().toLowerCase(), order.getOrigQty(), order.getAvgPrice(), getTime(order.getUpdateTime()));
        } else if (FILLED.toString().equals(order.getStatus())) {
            message = String.format("Order to %s %s by %s was executed at %s",
                    order.getSide().toLowerCase(), order.getExecutedQty(), order.getCumQty(), getTime(order.getUpdateTime()));
        } else {
            message = String.format("!!!! Order to %s %s by %s was executed at %s, status %s",
                    order.getSide().toLowerCase(), order.getOrigQty(), order.getAvgPrice(), getTime(order.getUpdateTime()), order.getStatus());
        }
        TelegramMessenger.send(symbol, message);
    }

    private void processOrderStatus(Order order, CandlestickInterval interval) {
        OrderStatus initialStatus = status;
        status = OrderStatus.valueOf(order.getStatus());

        if (status.equals(NEW)) {
            rebuyOrder(order); //TODO: correct logging
        } else if (initialStatus.equals(NEW) && status.equals(FILLED)) {
            String message = String.format("order was executed at %s", getTime(order.getUpdateTime()));
            TelegramMessenger.send(symbol, message);
        } else if (status.equals(PARTIALLY_FILLED)) {
            String message = String.format("order is partially filled %s", order.getUpdateTime());
            TelegramMessenger.send(symbol, message);
            Long updateTime = order.getUpdateTime();
            if (baseTime.equals(0L)) {
                baseTime = updateTime;
            } else {
                long difference = updateTime - baseTime;
                Long intervalInMilliSeconds = candleStickIntervalToMilliseconds(interval);
                if (difference >= (intervalInMilliSeconds / 2.0)) {
                    cancelOrder();
//                    isActive = true; // TODO: ???
                }
            }
        }
//        else { // FULL. since in NEW we don't go in the function.
//            isActive = true;
//        }
    }

    private synchronized void rebuyOrder(Order order) {
        try {
            log.info("{} rebuyOrder, order={}", symbol, order);
            ResponseResult rr = getRequestClient().cancelAllOpenOrder(symbol);
            log.info("{} All orders were cancelled with result {} {}", symbol, rr.getCode(), rr.getMsg());
            OrderSide side = OrderSide.valueOf(order.getSide());
            String qty = order.getOrigQty().toString();
            Order buyOrder = getRequestClient().postOrder(symbol, side, MARKET, qty, String.valueOf(order.getPrice()));
            updatePositionInfo(buyOrder);
            TelegramMessenger.send(symbol, "repeat order " + (BUY.toString().equals(buyOrder.getSide()) ? LONG : SHORT)
                    + ", " + buyOrder.getExecutedQty() + " by avgPrice=" + buyOrder.getActivatePrice() + ", status=" + buyOrder.getStatus());
            log.info("{} rebuying order is placed {}", symbol, buyOrder);
        } catch (Exception e) {
            log.error("{} Error during rebuying order", symbol, e);
        }
    }

    private synchronized void cancelOrder() {
        try {
            Order order = getRequestClient().cancelOrder(symbol, orderID, clientOrderId);
            TelegramMessenger.send(symbol, "cancel order, {}" + order);
        } catch (Exception e) {
            log.error("{} Error during canceling order", symbol, e);
        }
    }

    private void closePosition(SellingInstructions sellingInstructions, RealTimeData realTimeData) {
        String sellingQty = fixQuantity(formatQty(percentageOfQuantity(sellingInstructions.sellingQtyPercentage()), symbol));
        String currentPrice = realTimeData.getCurrentPrice().toString();
        log.info("{} closePosition with type {}", symbol, sellingInstructions.type());
        try {
            Order order = switch (sellingInstructions.type()) {
                case CLOSE_LONG_LIMIT -> getRequestClient().postOrder(symbol, SELL, LIMIT, sellingQty, currentPrice);
                case CLOSE_LONG_MARKET -> getRequestClient().postOrder(symbol, SELL, MARKET, sellingQty, null);
                case CLOSE_SHORT_LIMIT -> getRequestClient().postOrder(symbol, BUY, LIMIT, sellingQty, currentPrice);
                case CLOSE_SHORT_MARKET -> getRequestClient().postOrder(symbol, BUY, MARKET, sellingQty, null);
            };
            updatePositionInfo(order);
//            TelegramMessenger.send(symbol, "Order to close position was placed. Selling price: " + currentPrice);
        } catch (Exception e) {
            log.error("Exception during closePosition: ", e);
            TelegramMessenger.send(symbol, "Exception happened");
        }
    }

    private double percentageOfQuantity(double percentage) {
        return (qty * percentage) / 100;
    }

    public enum ClosePositionTypes {
        CLOSE_LONG_MARKET,
        CLOSE_LONG_LIMIT,
        CLOSE_SHORT_MARKET,
        CLOSE_SHORT_LIMIT
    }
}
