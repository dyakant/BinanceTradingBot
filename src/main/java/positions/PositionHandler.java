package positions;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.trade.Order;
import data.AccountBalance;
import data.Config;
import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import singletonHelpers.BinanceInfo;
import singletonHelpers.RequestClient;
import singletonHelpers.TelegramMessenger;
import strategies.ExitStrategy;
import utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.OrderType.MARKET;
import static com.binance.client.model.enums.PositionSide.BOTH;
import static com.binance.client.model.enums.TimeInForce.GTC;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static data.Config.REDUCE_ONLY;

@Slf4j
public class PositionHandler implements Serializable {
    private String clientOrderId;
    private Long orderID;
    private double qty = 0;
    private final String symbol;
    private volatile boolean isActive = false;
    private String status = Config.NEW;
    private final ArrayList<ExitStrategy> exitStrategies;
    private Long baseTime = 0L;
    private volatile boolean rebuying = true;
    private volatile boolean isSelling = false;
    private volatile boolean terminated = false;

    public PositionHandler(Order order, ArrayList<ExitStrategy> _exitStrategies) {
        clientOrderId = order.getClientOrderId();
        orderID = order.getOrderId();
        symbol = order.getSymbol().toLowerCase();
        exitStrategies = _exitStrategies;
    }

    public synchronized boolean isSoldOut() {
        return isActive && isSelling && (!status.equals(Config.NEW)) && (!rebuying) && ((qty == 0));
    }

    public synchronized void run(DataHolder realTimeData) {
        isSelling = false;
        for (ExitStrategy exitStrategy : exitStrategies) {
            SellingInstructions sellingInstructions = exitStrategy.run(realTimeData);
            if ((!isSelling) && sellingInstructions != null) {
                TelegramMessenger.send(symbol, "close position by the strategy " + sellingInstructions);
                isSelling = true;
                closePosition(sellingInstructions, realTimeData);
                break;
            }
        }
    }

    public synchronized void update(DataHolder realTimeData, CandlestickInterval interval) {
        rebuying = false;
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        Order order = syncRequestClient.getOrder(symbol, orderID, clientOrderId);
        status = order.getStatus();
        isActive(realTimeData, order, interval);
        qty = AccountBalance.getAccountBalance().getPosition(symbol).getPositionAmt().doubleValue();
    }

    private void isActive(DataHolder realTimeData, Order order, CandlestickInterval interval) {
        if (status.equals(Config.NEW)) {
            rebuyOrder(realTimeData, order);
        } else if (status.equals(Config.PARTIALLY_FILLED)) {
            Long updateTime = order.getUpdateTime();
            if (baseTime.equals(0L)) {
                baseTime = updateTime;
            } else {
                long difference = updateTime - baseTime;
                Long intervalInMilliSeconds = Utils.candleStickIntervalToMilliseconds(interval);
                if (difference >= (intervalInMilliSeconds / 2.0)) {
                    SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
                    syncRequestClient.cancelOrder(symbol, orderID, clientOrderId);
                    isActive = true;
                }
            }
        } else { // FULL. since in NEW we don't go in the function.
            isActive = true;
        }
    }

    private synchronized void rebuyOrder(DataHolder realTimeData, Order order) {
        rebuying = true;
        try {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            syncRequestClient.cancelAllOpenOrder(symbol);
            OrderSide side = stringToOrderSide(order.getSide());
            Order buyOrder = syncRequestClient.postOrder(symbol, side, null, MARKET, null,
                    order.getOrigQty().toString(), null, null, null, null, null, null, null, null, MARK_PRICE.toString(), RESULT);
            TelegramMessenger.send(symbol, "bought again:  " + buyOrder);
            clientOrderId = buyOrder.getClientOrderId();
            orderID = buyOrder.getOrderId();
        } catch (Exception ignored) {
        }
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
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            syncRequestClient.cancelAllOpenOrder(symbol);
            TelegramMessenger.send(symbol, "Position closed!, balance:  " + AccountBalance.getAccountBalance().getCoinBalance("usdt"));
        }
    }

    private void closePosition(SellingInstructions sellingInstructions, DataHolder realTimeData) {
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        String sellingQty = Utils.fixQuantity(BinanceInfo.formatQty(percentageOfQuantity(sellingInstructions.getSellingQtyPercentage()), symbol));
        try {
            Order order = null;
            String currentPrice = realTimeData.getCurrentPrice().toString();
            switch (sellingInstructions.getType()) {

                case STAY_IN_POSITION:
                    break;

                case SELL_LIMIT:
                    order = syncRequestClient.postOrder(symbol, SELL, BOTH, LIMIT, GTC, sellingQty, currentPrice,
                            REDUCE_ONLY, null, null, null, null,
                            null, MARK_PRICE, null, RESULT);
                    break;

                case SELL_MARKET:
                    order = syncRequestClient.postOrder(symbol, SELL, BOTH, MARKET, null, sellingQty, null,
                            REDUCE_ONLY, null, null, null, null,
                            null, null, null, RESULT);
                    break;

                case CLOSE_SHORT_LIMIT:
                    order = syncRequestClient.postOrder(symbol, BUY, BOTH, LIMIT, GTC, sellingQty, currentPrice,
                            REDUCE_ONLY, null, null, null, null,
                            null, MARK_PRICE, null, RESULT);
                    break;

                case CLOSE_SHORT_MARKET:
                    order = syncRequestClient.postOrder(symbol, BUY, BOTH, MARKET, null, sellingQty, null,
                            REDUCE_ONLY, null, null, null, null,
                            null, null, null, RESULT);
                    break;

                default:
            }
            if (Objects.nonNull(order)) {
                log.info("{}. order: {}", sellingInstructions.getType(), order);
                TelegramMessenger.send(symbol, "Selling price:  " + currentPrice);
            } else {
                log.info("{}. Order is empty", sellingInstructions.getType());
                TelegramMessenger.send(symbol, "Not done. " + sellingInstructions.getType());
            }
        } catch (Exception ex) {
            log.info(ex.toString());
            TelegramMessenger.send(symbol, "Exception happened");
        }
    }

    public enum ClosePositionTypes {
        STAY_IN_POSITION,
        SELL_MARKET,
        SELL_LIMIT,
        CLOSE_SHORT_MARKET,
        CLOSE_SHORT_LIMIT
    }
}
