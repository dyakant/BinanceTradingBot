package codeExecution;

import com.binance.client.model.enums.CandlestickInterval;
import singletonHelpers.BinanceInfo;
import strategies.EntryStrategy;
import strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy;
import strategies.rsiStrategies.RSIEntryStrategy;

public class InputMessage {
    public String operation = RealTImeOperations.UNKNOWN_OPERATION;
    private String symbol;
    private CandlestickInterval interval;
    private EntryStrategy entryStrategy;
    private String apiKey;
    private String secretKey;

    public void initialize(String input) {
        String[] messageParts = input.split(" ");
        operation = messageParts[0];
        switch (operation) {
            case RealTImeOperations.CANCEL_ALL_ORDERS:

            case RealTImeOperations.GET_LAST_TRADES:

            case RealTImeOperations.GET_OPEN_ORDERS:

            case RealTImeOperations.GET_CURRENT_BALANCE:

            case RealTImeOperations.SHOW_STRATEGIES:
                if (messageParts.length != 2) {
                    System.out.println("provide necessary parameters");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                symbol = messageParts[1];
                if (!BinanceInfo.isSymbolExists(symbol)) {
                    System.out.println("Wrong symbol");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                }
                break;

            case RealTImeOperations.SHOW_ALL_STRATEGIES:

            case RealTImeOperations.CLOSE_ALL_POSITIONS:

            case RealTImeOperations.CLOSE_PROGRAM:

            case RealTImeOperations.GET_OPEN_POSITIONS:
                break;

            case RealTImeOperations.ACTIVATE_STRATEGY:

            case RealTImeOperations.DEACTIVATE_STRATEGY:
                if (messageParts.length != 4) {
                    System.out.println("provide necessary parameters");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                symbol = messageParts[1];
                if (!BinanceInfo.isSymbolExists(symbol)) {
                    System.out.println("Wrong symbol");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                entryStrategy = stringToEntryStrategy(messageParts[2]);
                if (entryStrategy == null) {
                    System.out.println("This strategy don't exists");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                interval = null;
                for (CandlestickInterval candlestickInterval : CandlestickInterval.values()) {
                    if (candlestickInterval.toString().equals(messageParts[3])) interval = candlestickInterval;
                }
                if (interval == null) {
                    System.out.println("Wrong interval");
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                break;

            case "help":
                System.out.println("""
                        Optional commands:
                        cao [symbol] - Cancel all orders, for [symbol]
                        cap - Close all open positions
                        as [symbol] [strategy] [interval] - For [symbol] activate strategy [strategy] and candlestick interval [interval]
                        ds [symbol] [strategy] [interval] - For [symbol] deactivate strategy [strategy] and candlestick interval [interval]
                        ss [symbol] - Show all strategies for [symbol]
                        sas - Show all strategies
                        glt [symbol] - Get last trades for [symbol]
                        gop - get all Open positions
                        goo [symbol] - Get all open orders for [symbol]
                        gcb [symbol] - Get current balance for [symbol]
                        cp - Close program

                         entryStrategy options: rsi, macd
                         interval options: 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h ,8h, 12h, 1d, 3d, 1w, 1M"""
                );
                break;

            default:
                System.out.println("Wrong operation");
                operation = RealTImeOperations.UNKNOWN_OPERATION;
        }
    }

    private EntryStrategy stringToEntryStrategy(String strategyName) {
        return switch (strategyName) {
            case "rsi" -> new RSIEntryStrategy();
            case "macd" -> new MACDOverRSIEntryStrategy();
            default -> null;
        };
    }

    public String getSymbol() {
        return symbol;
    }

    public String getOperation() {
        return operation;
    }

    public CandlestickInterval getInterval() {
        return interval;
    }

    public EntryStrategy getEntryStrategy() {
        return entryStrategy;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
