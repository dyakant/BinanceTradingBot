package com.btb.codeExecution;

import com.binance.client.SubscriptionClient;
import com.binance.client.model.enums.CandlestickInterval;
import com.btb.data.AccountBalance;
import com.btb.data.RealTimeData;
import com.btb.positions.PositionHandler;
import com.btb.singletonHelpers.ExecService;
import com.btb.singletonHelpers.SubClient;
import com.btb.strategies.EntryStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
public class InvestmentManager implements Runnable {
    private final CandlestickInterval interval;
    private final String symbol;
    private final String strategyName;
    ConcurrentLinkedDeque<EntryStrategy> entryStrategies;
    ConcurrentLinkedDeque<PositionHandler> positionHandlers;
    ConcurrentLinkedDeque<Future<?>> futures;

    public InvestmentManager(CandlestickInterval interval, String symbol, EntryStrategy entryStrategy) {
        this.interval = interval;
        this.symbol = symbol;
        strategyName = entryStrategy.getName();
        entryStrategies = new ConcurrentLinkedDeque<>();
        positionHandlers = new ConcurrentLinkedDeque<>();
        futures = new ConcurrentLinkedDeque<>();
        entryStrategies.add(entryStrategy);
    }

    @Override
    public void run() {
        RealTimeData realTimeData = new RealTimeData(symbol, interval);
        SubscriptionClient subscriptionClient = SubClient.getSubClient().getSubscriptionClient();
        ExecutorService iterationExecutorService = ExecService.getExecService().getExecutorService();

        subscriptionClient.subscribeCandlestickEvent(symbol, interval,
                ((event) -> iterationExecutorService.execute(() -> {
                    boolean updated = realTimeData.updateData(symbol, event);
                    if (updated) {
                        AccountBalance.getAccountBalance().updateBalance();
                        for (PositionHandler positionHandler : positionHandlers) {
                            positionHandler.update(interval);
                            if (positionHandler.shouldStopTrading()) {
                                positionHandler.stopTrading();
                                positionHandlers.remove(positionHandler);
                            } else {
                                positionHandler.process(realTimeData);
                            }
                        }
                        for (EntryStrategy entryStrategy : entryStrategies) {
                            PositionHandler positionHandler = entryStrategy.run(realTimeData);
                            if (positionHandler != null) {
                                positionHandlers.add(positionHandler);
                            }
                        }
                    }
                })),
                e -> log.error("subscribeCandlestickEvent exception:", e));
    }

    public void addEntryStrategy(EntryStrategy entryStrategy) {
        entryStrategies.add(entryStrategy);
    }

    public void removeEntryStrategy(EntryStrategy entryStrategy) {
        entryStrategies.remove(entryStrategy);
    }

    public String getStrategyName() {
        return strategyName;
    }
}
