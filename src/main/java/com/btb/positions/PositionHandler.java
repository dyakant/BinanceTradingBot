package com.btb.positions;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.trade.Order;
import com.btb.data.AccountBalance;
import com.btb.data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import com.btb.singletonHelpers.BinanceInfo;
import com.btb.singletonHelpers.RequestClient;
import com.btb.singletonHelpers.TelegramMessenger;
import com.btb.strategies.ExitStrategy;
import com.btb.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.OrderType.MARKET;
import static com.binance.client.model.enums.PositionSide.*;
import static com.binance.client.model.enums.TimeInForce.GTC;
import static com.binance.client.model.enums.WorkingType.CONTRACT_PRICE;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static com.btb.data.Config.*;
import static com.btb.utils.Utils.candleStickIntervalToMilliseconds;
import static com.btb.utils.Utils.getTime;

@Slf4j
public class PositionHandler implements Serializable {
    private final SyncRequestClient syncRequestClient;
    private final AccountBalance accountBalance;
    private String clientOrderId;
    private Long orderID;
    private double qty = 0;
    private final String symbol;
    private volatile boolean isActive = false;
    private String status = NEW;
    private final ArrayList<ExitStrategy> exitStrategies;
    private Long baseTime = 0L;
    private volatile boolean rebuying = true;
    private volatile boolean isSelling = false;
    private volatile boolean terminated = false;

    public PositionHandler(Order order, ArrayList<ExitStrategy> _exitStrategies) {
        syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        accountBalance = AccountBalance.getAccountBalance();
        clientOrderId = order.getClientOrderId();
        orderID = order.getOrderId();
        symbol = order.getSymbol().toLowerCase();
        exitStrategies = _exitStrategies;
    }

    public synchronized boolean isSoldOut() {
        return isActive && isSelling && (!status.equals(NEW)) && (!rebuying) && (qty == 0);
    }

    public synchronized void run(DataHolder realTimeData) {
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

    public synchronized void update(DataHolder realTimeData, CandlestickInterval interval) {
        rebuying = false;
        try {
            Order order = syncRequestClient.getOrder(symbol, orderID, clientOrderId);
            log.info("{} Update order information, order={}", symbol, order);
            if (NEW.equals(status) && FILLED.equals(order.getStatus())) { // TODO: Place it in isActive()
                String message = String.format("order was executed at %s", getTime(order.getUpdateTime()));
                TelegramMessenger.send(symbol, message);
                log.info("{} status changed from NEW to {}", symbol, order.getStatus());
            }
            status = order.getStatus();
            isActive(realTimeData, order, interval);
            qty = accountBalance.getPosition(symbol).getPositionAmt().doubleValue();
        } catch (Exception e) {
            log.error("Error during update position status", e);
        }
    }

    private void isActive(DataHolder realTimeData, Order order, CandlestickInterval interval) {
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
            ResponseResult rr = syncRequestClient.cancelAllOpenOrder(symbol);
            log.info("{} All orders were cancelled with result {} {}", symbol, rr.getCode(), rr.getMsg());
            OrderSide side = stringToOrderSide(order.getSide());
            Order buyOrder = postOrder(side, order.getOrigQty().toString());
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
            Order order = syncRequestClient.cancelOrder(symbol, orderID, clientOrderId);
            log.info("{} cancel order, order: {}", symbol, order);
            TelegramMessenger.send(symbol, "cancel order, {}" + order);
        } catch (Exception e) {
            log.error("{} Error during canceling order", symbol, e);
        }
    }

    private Order postOrder(OrderSide side, String origQty) {
        return syncRequestClient.postOrder(
                symbol,
                side,
                null,
                MARKET,
                null,
                origQty,
                null,
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

    private OrderSide stringToOrderSide(String side) {
        for (OrderSide orderSide : OrderSide.values()) {
            if (orderSide.toString().equals(side)) return orderSide;
        }
        return null;
    }

    private double percentageOfQuantity(double percentage) {
        return qty * percentage;
    }

    public void terminate() {
        if (!terminated) {
            terminated = true;
            syncRequestClient.cancelAllOpenOrder(symbol);
            TelegramMessenger.send(symbol, "Position closed!, balance:  " + AccountBalance.getBalanceUsdt());
        }
    }

    private void closePosition(SellingInstructions sellingInstructions, DataHolder realTimeData) {
        String sellingQty = Utils.fixQuantity(BinanceInfo.formatQty(percentageOfQuantity(sellingInstructions.getSellingQtyPercentage()), symbol));
        try {
            Order order = null;
            String currentPrice = realTimeData.getCurrentPrice().toString();
            log.info("{} closePosition with type {}", symbol, sellingInstructions.getType());
            switch (sellingInstructions.getType()) {

                case STAY_IN_POSITION:
                    break;

                case CLOSE_LONG_LIMIT:
                    order = syncRequestClient.postOrder(symbol, SELL, BOTH, LIMIT, GTC, sellingQty, currentPrice,
                            REDUCE_ONLY, null, null, null, null,
                            null, MARK_PRICE, null, RESULT);
                    break;

                case CLOSE_LONG_MARKET:
                    order = syncRequestClient.postOrder(symbol, SELL, BOTH, MARKET, null, sellingQty, null,
                            null, null, null, null, null,
                            null, CONTRACT_PRICE, null, RESULT);
                    break;

                case CLOSE_SHORT_LIMIT:
                    order = syncRequestClient.postOrder(symbol, BUY, BOTH, LIMIT, GTC, sellingQty, currentPrice,
                            REDUCE_ONLY, null, null, null, null,
                            null, MARK_PRICE, null, RESULT);
                    break;

                case CLOSE_SHORT_MARKET:
                    order = syncRequestClient.postOrder(symbol, BUY, BOTH, MARKET, null, sellingQty, null,
                            null, null, null, null, null,
                            null, CONTRACT_PRICE, null, RESULT);
                    break;

                default:
            }
            if (Objects.nonNull(order)) {
                log.info("{} position closed, order: {}", symbol, order);
                TelegramMessenger.send(symbol, "Order to close position was placed. Selling price:  " + currentPrice);
            } else {
                log.info("{} position not closed, order is empty", symbol);
                TelegramMessenger.send(symbol, "Not done. " + sellingInstructions.getType());
            }
        } catch (Exception e) {
            TelegramMessenger.send(symbol, "Exception happened");
            log.error("Exception during closePosition: ", e);
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
