package com.btb.singletonHelpers;

import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.Leverage;
import com.binance.client.model.trade.MyTrade;
import com.binance.client.model.trade.Order;
import com.btb.data.Config;

import java.util.List;

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.PositionSide.BOTH;
import static com.binance.client.model.enums.TimeInForce.GTC;
import static com.binance.client.model.enums.WorkingType.CONTRACT_PRICE;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static com.btb.data.Config.REDUCE_ONLY;

public class RequestClient {
    private final SyncRequestClient syncRequestClient;

    private static class RequestClientHolder {
        private static final RequestClient RequestClient = new RequestClient();
    }

    private RequestClient() {
        RequestOptions options = new RequestOptions();
        syncRequestClient = SyncRequestClient.create(Config.API_KEY, Config.SECRET_KEY, options);
    }

    public static RequestClient getRequestClient() {
        return RequestClientHolder.RequestClient;
    }

    public SyncRequestClient getSyncRequestClient() {
        return syncRequestClient;
    }

    public Leverage changeInitialLeverage(String symbol, Integer leverage) {
        return syncRequestClient.changeInitialLeverage(symbol, leverage);
    }

    public Order postLimitOrder(String symbol, OrderSide side, String quantity, String price) {
        return sendPostOrder(symbol, side, LIMIT, null, quantity, price );
    }

    public Order postOrder(String symbol, OrderSide side, OrderType orderType, String quantity, String price) {
        return sendPostOrder(symbol,
                side,
                orderType,
                LIMIT.equals(orderType) ? REDUCE_ONLY : null,
                quantity,
                LIMIT.equals(orderType) ? price : null);
    }

    public Order sendPostOrder(String symbol, OrderSide side, OrderType orderType, String reduceOnly, String quantity, String price) {
        return syncRequestClient.postOrder(
                symbol,
                side,
                BOTH,
                orderType,
                LIMIT.equals(orderType) ? GTC : null,
                quantity,
                price,
                reduceOnly,
                null,
                null,
                null,
                null,
                null,
                LIMIT.equals(orderType) ? MARK_PRICE : CONTRACT_PRICE,
                null,
                RESULT);
    }

    public Order getOrder(String symbol, Long orderID, String clientOrderId) {
        return syncRequestClient.getOrder(symbol, orderID, clientOrderId);
    }

    public Order cancelOrder(String symbol, Long orderID, String clientOrderId) {
        return syncRequestClient.cancelOrder(symbol, orderID, clientOrderId);
    }

    public ResponseResult cancelAllOpenOrder(String symbol) {
        return syncRequestClient.cancelAllOpenOrder(symbol);
    }

    public List<MyTrade> getAccountTrades(String symbol) {
        return syncRequestClient.getAccountTrades(symbol, null, null, null, 100);
    }

    public List<Order> getOpenOrders(String symbol) {
        return syncRequestClient.getOpenOrders(symbol);
    }
}
