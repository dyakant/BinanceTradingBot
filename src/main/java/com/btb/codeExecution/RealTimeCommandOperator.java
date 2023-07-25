package com.btb.codeExecution;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.MyTrade;
import com.binance.client.model.trade.Order;
import com.binance.client.model.trade.Position;
import com.btb.data.AccountBalance;
import com.btb.data.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.btb.singletonHelpers.ExecService;
import com.btb.singletonHelpers.RequestClient;
import com.btb.singletonHelpers.SubClient;
import com.btb.singletonHelpers.TelegramMessenger;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class RealTimeCommandOperator {
    private final HashMap<String, RealTimeOperation> commandsAndOps;
    private final HashMap<Pair<String, CandlestickInterval>, InvestmentManager> investmentManagerHashMap;
    private final ReadWriteLock investmentManagerHashMapLock = new ReentrantReadWriteLock();

    public RealTimeCommandOperator() {
        investmentManagerHashMap = new HashMap<>();
        commandsAndOps = new HashMap<>();

        commandsAndOps.put(RealTImeOperations.CANCEL_ALL_ORDERS, (message) -> {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            syncRequestClient.cancelAllOpenOrder(message.getSymbol());
        });

        commandsAndOps.put(RealTImeOperations.CLOSE_ALL_POSITIONS, (message) -> {
            List<Position> openPositions = AccountBalance.getAccountBalance().getOpenPositions();
            for (Position openPosition : openPositions) {
                SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
                if (!openPosition.getPositionSide().equals("SHORT")) {
                    syncRequestClient.postOrder(openPosition.getSymbol().toLowerCase(), OrderSide.SELL, null, OrderType.MARKET, null,
                            openPosition.getPositionAmt().toString(), null, Config.REDUCE_ONLY, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
                } else {
                    syncRequestClient.postOrder(openPosition.getSymbol().toLowerCase(), OrderSide.BUY, null, OrderType.MARKET, null,
                            openPosition.getPositionAmt().toString(), null, Config.REDUCE_ONLY, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
                }
            }
        });

        commandsAndOps.put(RealTImeOperations.ACTIVATE_STRATEGY, (message) -> {
            Pair<String, CandlestickInterval> pair = new MutablePair<>(message.getSymbol(), message.getInterval());
            investmentManagerHashMapLock.readLock().lock();
            if (investmentManagerHashMap.containsKey(pair)) {
                investmentManagerHashMap.get(pair).addEntryStrategy(message.getEntryStrategy());
                investmentManagerHashMapLock.readLock().unlock();
            } else {
                investmentManagerHashMapLock.readLock().unlock();
                investmentManagerHashMapLock.writeLock().lock();
                InvestmentManager investmentManager = new InvestmentManager(message.getInterval(), message.getSymbol(), message.getEntryStrategy());
                investmentManagerHashMap.put(pair, investmentManager);
                investmentManagerHashMapLock.writeLock().unlock();
                TelegramMessenger.send(message.getSymbol(), "activate strategy '" + message.getEntryStrategy().getName() +
                        " / " + message.getInterval() + "', balance: " + AccountBalance.getBalanceUsdt());
                investmentManager.run();
            }
        });

        commandsAndOps.put(RealTImeOperations.DEACTIVATE_STRATEGY, (message) -> {
            Pair<String, CandlestickInterval> pair = new MutablePair<>(message.getSymbol(), message.getInterval());
            investmentManagerHashMapLock.readLock().lock();
            if (investmentManagerHashMap.containsKey(pair)) {
                investmentManagerHashMap.get(pair).removeEntryStrategy(message.getEntryStrategy());
            }
            investmentManagerHashMapLock.readLock().unlock();
        });

        commandsAndOps.put(RealTImeOperations.SHOW_STRATEGIES, (message) -> {
            investmentManagerHashMapLock.readLock().lock();
            List<String> list = investmentManagerHashMap.entrySet().stream()
                    .filter((k) -> k.getKey().getKey().equals(message.getSymbol()))
                    .map(o -> o.getKey() + ": " + o.getValue().getStrategyName() + " / " + o.getKey().getValue())
                    .toList();
            TelegramMessenger.send(list.toString());
            investmentManagerHashMapLock.readLock().unlock();
        });

        commandsAndOps.put(RealTImeOperations.SHOW_ALL_STRATEGIES, (message) -> {
            investmentManagerHashMapLock.readLock().lock();
            List<String> list = investmentManagerHashMap.entrySet().stream()
                    .map(o -> o.getKey() + ": " + o.getValue().getStrategyName() + " / " + o.getKey().getValue())
                    .toList();
            TelegramMessenger.send(list.toString());
            investmentManagerHashMapLock.readLock().unlock();
        });

        commandsAndOps.put(RealTImeOperations.GET_LAST_TRADES, (message) -> {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            List<MyTrade> myTrades = syncRequestClient.getAccountTrades(message.getSymbol(), null, null, null, 100);
            int index = 1;
            for (MyTrade trade : myTrades) {
                System.out.println("Trade " + index + ": " + trade);
                index++;
            }
        });

        commandsAndOps.put(RealTImeOperations.GET_OPEN_POSITIONS, (message) -> {
            List<Position> openPositions = AccountBalance.getAccountBalance().getOpenPositions();
            int index = 1;
            for (Position openPosition : openPositions) {
                System.out.println("Open position " + index + ": " + openPosition);
                index++;
            }
        });

        commandsAndOps.put(RealTImeOperations.GET_OPEN_ORDERS, (message) -> {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            List<Order> openOrders = syncRequestClient.getOpenOrders(message.getSymbol());
            int index = 1;
            for (Order openOrder : openOrders) {
                System.out.println("Open order: " + index + ": " + openOrder);
                index++;
            }
        });

        commandsAndOps.put(RealTImeOperations.GET_CURRENT_BALANCE, (message) ->
                TelegramMessenger.send("Your current balance is: " + AccountBalance.getBalanceUsdt()));

        commandsAndOps.put(RealTImeOperations.CLOSE_PROGRAM, (message) -> {
            SubClient.getSubClient().getSubscriptionClient().unsubscribeAll();
            ExecutorService executorService = ExecService.getExecService().getExecutorService();
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        });
    }

    public HashMap<String, RealTimeOperation> getCommandsAndOps() {
        return commandsAndOps;
    }
}
