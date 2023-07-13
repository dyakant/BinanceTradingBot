package codeExecution;

import com.binance.client.SubscriptionClient;
import com.binance.client.model.enums.CandlestickInterval;
import data.AccountBalance;
import data.DataHolder;
import data.RealTimeData;
import positions.PositionHandler;
import singletonHelpers.ExecService;
import singletonHelpers.SubClient;
import singletonHelpers.TelegramMessenger;
import strategies.EntryStrategy;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class InvestmentManager implements Runnable {
    private final CandlestickInterval interval;
    private final String symbol;
    ConcurrentLinkedDeque<EntryStrategy> entryStrategies;
    ConcurrentLinkedDeque<PositionHandler> positionHandlers;
    ConcurrentLinkedDeque<Future<?>> futures;


    public InvestmentManager(CandlestickInterval interval, String symbol, EntryStrategy entryStrategy) {
        this.interval = interval;
        this.symbol = symbol;
        entryStrategies = new ConcurrentLinkedDeque<>();
        positionHandlers = new ConcurrentLinkedDeque<>();
        futures = new ConcurrentLinkedDeque<>();
        entryStrategies.add(entryStrategy);
    }

    public void run() {
        RealTimeData realTimeData = new RealTimeData(symbol, interval);
        SubscriptionClient subscriptionClient = SubClient.getSubClient().getSubscriptionClient();
        ExecutorService iterationExecutorService = ExecService.getExecService().getExecutorService();
        TelegramMessenger.sendToTelegram(symbol + " balance:  " + AccountBalance.getAccountBalance().getCoinBalance("usdt"));

        subscriptionClient.subscribeCandlestickEvent(symbol, interval,
                ((event) -> iterationExecutorService.execute(() -> {
                    DataHolder dataHolder = realTimeData.updateData(event);
                    if (dataHolder != null) {
                        AccountBalance.getAccountBalance().updateBalance();
                        for (PositionHandler positionHandler : positionHandlers) {
                            positionHandler.update(dataHolder, interval);
                            if (positionHandler.isSoldOut()) {
                                positionHandler.terminate();
                                positionHandlers.remove(positionHandler);
                            } else {
                                positionHandler.run(dataHolder);
                            }
                        }
                        for (EntryStrategy entryStrategy : entryStrategies) {
                            PositionHandler positionHandler = entryStrategy.run(dataHolder, symbol);
                            if (positionHandler != null) {
                                positionHandlers.add(positionHandler);
                            }
                        }
                    }
                })),
                System.out::println);
    }

    public void addEntryStrategy(EntryStrategy entryStrategy) {
        entryStrategies.add(entryStrategy);
    }

    public void removeEntryStrategy(EntryStrategy entryStrategy) {
        entryStrategies.remove(entryStrategy);
    }

}
