package com.btb.codeExecution;

import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.CandlestickInterval;
import com.binance.client.model.trade.MyTrade;
import com.binance.client.model.trade.Order;
import com.binance.client.model.trade.Position;
import com.btb.data.Account;
import com.btb.singletonHelpers.ExecService;
import com.btb.singletonHelpers.RequestClient;
import com.btb.singletonHelpers.SubClient;
import com.btb.singletonHelpers.TelegramMessenger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.MARKET;
import static com.binance.client.model.enums.PositionSide.LONG;

@Slf4j
public class RealTimeCommandOperator {
    private final RequestClient requestClient = RequestClient.getRequestClient();
    private final HashMap<String, RealTimeOperation> commandsAndOps;
    private final HashMap<Pair<String, CandlestickInterval>, InvestmentManager> investmentManagerHashMap;
    private final ReadWriteLock investmentManagerHashMapLock = new ReentrantReadWriteLock();

    public RealTimeCommandOperator() {
        investmentManagerHashMap = new HashMap<>();
        commandsAndOps = new HashMap<>();

        commandsAndOps.put(RealTImeOperations.CANCEL_ALL_ORDERS, (message) -> {
            ResponseResult result = requestClient.cancelAllOpenOrder(message.getSymbol());
            TelegramMessenger.send("Orders were canceled: " + result);
        });

        commandsAndOps.put(RealTImeOperations.CLOSE_ALL_POSITIONS, (message) -> {
            List<Position> openPositions = Account.getAccount().getOpenPositions();
            for (Position openPosition : openPositions) {
                if (LONG.toString().equals(openPosition.getPositionSide())) {
                    requestClient.postOrder(openPosition.getSymbol().toLowerCase(), SELL, MARKET, openPosition.getPositionAmt().toString(), null);
                } else {
                    requestClient.postOrder(openPosition.getSymbol().toLowerCase(), BUY, MARKET, openPosition.getPositionAmt().toString(), null);
                }
            }
        });

        commandsAndOps.put(RealTImeOperations.ACTIVATE_STRATEGY, (message) -> {
            Pair<String, CandlestickInterval> pair = new MutablePair<>(message.getSymbol(), message.getInterval());
            investmentManagerHashMapLock.readLock().lock();
            if (investmentManagerHashMap.containsKey(pair)) {
                if (investmentManagerHashMap.get(pair).getStrategyName().equals(message.getEntryStrategy().getName())) {
                    investmentManagerHashMapLock.readLock().unlock();
                    TelegramMessenger.send(message.getSymbol(), "this strategy is already added");
                } else {
                    investmentManagerHashMap.get(pair).addEntryStrategy(message.getEntryStrategy());
                    investmentManagerHashMapLock.readLock().unlock();
                }
            } else {
                investmentManagerHashMapLock.readLock().unlock();
                investmentManagerHashMapLock.writeLock().lock();
                InvestmentManager investmentManager = new InvestmentManager(message.getInterval(), message.getSymbol(), message.getEntryStrategy());
                investmentManagerHashMap.put(pair, investmentManager);
                investmentManagerHashMapLock.writeLock().unlock();
                TelegramMessenger.send(message.getSymbol(), "activate strategy '" + message.getEntryStrategy().getName() +
                        " / " + message.getInterval() + "'");
                investmentManager.run();
            }
        });

        commandsAndOps.put(RealTImeOperations.DEACTIVATE_STRATEGY, (message) -> {
            Pair<String, CandlestickInterval> pair = new MutablePair<>(message.getSymbol(), message.getInterval());
            investmentManagerHashMapLock.readLock().lock();
            if (investmentManagerHashMap.containsKey(pair)) {
                log.debug("Remove strategy");
                investmentManagerHashMap.get(pair).removeEntryStrategy(message.getEntryStrategy());
            }
            investmentManagerHashMapLock.readLock().unlock();
            TelegramMessenger.send(message.getSymbol(), "strategy '" + message.getEntryStrategy().getName() +
                    " / " + message.getInterval() + "' removed");
        });

        commandsAndOps.put(RealTImeOperations.SHOW_STRATEGIES, (message) -> {
            investmentManagerHashMapLock.readLock().lock();
            List<String> list = investmentManagerHashMap.entrySet().stream()
                    .filter((k) -> k.getKey().getKey().equals(message.getSymbol()))
                    .map(o -> o.getKey() + ": " + o.getValue().getStrategyName() + " / " + o.getKey().getValue())
                    .toList();
            investmentManagerHashMapLock.readLock().unlock();
            TelegramMessenger.send("Active strategies:\n" + String.join("\n", list));
        });

        commandsAndOps.put(RealTImeOperations.SHOW_ALL_STRATEGIES, (message) -> {
            investmentManagerHashMapLock.readLock().lock();
            List<String> list = investmentManagerHashMap.entrySet().stream()
                    .map(o -> o.getKey().getKey() + ": " + o.getValue().getStrategyName() + " / " + o.getKey().getValue())
                    .toList();
            investmentManagerHashMapLock.readLock().unlock();
            TelegramMessenger.send("Active strategies:\n" + String.join("\n", list));
        });

        commandsAndOps.put(RealTImeOperations.GET_LAST_TRADES, (message) -> {
            List<MyTrade> list = requestClient.getAccountTrades(message.getSymbol());
            StringBuilder stringBuilder = new StringBuilder();
            int index = 1;
            stringBuilder.append("Last trades:\n");
            for (MyTrade trade : list) {
                stringBuilder.append(index++).append(": ").append(trade).append("\n");
            }
            TelegramMessenger.send(stringBuilder.toString());
        });

        commandsAndOps.put(RealTImeOperations.GET_OPEN_POSITIONS, (message) -> {
            List<Position> openPositions = Account.getAccount().getOpenPositions();
            StringBuilder stringBuilder = new StringBuilder();
            int index = 1;
            stringBuilder.append("Open positions:\n");
            for (Position openPosition : openPositions) {
                stringBuilder.append(index++).append(": ").append(openPosition).append("\n");
            }
            log.info(stringBuilder.toString());
            TelegramMessenger.send(stringBuilder.toString());
        });

        commandsAndOps.put(RealTImeOperations.GET_OPEN_ORDERS, (message) -> {
            List<Order> openOrders = requestClient.getOpenOrders(message.getSymbol());
            StringBuilder stringBuilder = new StringBuilder();
            int index = 1;
            stringBuilder.append("Open orders:\n");
            for (Order openOrder : openOrders) {
                stringBuilder.append(index++).append(": ").append(openOrder).append("\n");
            }
            TelegramMessenger.send(stringBuilder.toString());
        });

        commandsAndOps.put(RealTImeOperations.GET_CURRENT_BALANCE, (message) ->
                TelegramMessenger.send("Your current balance is: " + Account.getUsdtBalance()));

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
            TelegramMessenger.send("TelegramBot is closed.");
            System.exit(200);
        });
    }

    public HashMap<String, RealTimeOperation> getCommandsAndOps() {
        return commandsAndOps;
    }
}
