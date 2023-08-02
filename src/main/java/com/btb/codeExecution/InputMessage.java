package com.btb.codeExecution;

import com.binance.client.model.enums.CandlestickInterval;
import com.btb.singletonHelpers.BinanceInfo;
import com.btb.strategies.EntryStrategy;
import com.btb.strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy;
import com.btb.strategies.rsiStrategies.RSIEntryStrategy;

import static com.btb.strategies.EntryStrategyType.MACD;
import static com.btb.strategies.EntryStrategyType.RSI;

public class InputMessage {
    public String operation = RealTImeOperations.UNKNOWN_OPERATION;
    private String symbol;
    private CandlestickInterval interval;
    private EntryStrategy entryStrategy;

    public String processCommand(String input) {
        String returnValue = "";
        String[] messageParts = input.split(" ");
        operation = messageParts[0];
        switch (operation) {
            case RealTImeOperations.CANCEL_ALL_ORDERS:

            case RealTImeOperations.GET_LAST_TRADES:

            case RealTImeOperations.GET_OPEN_ORDERS:

            case RealTImeOperations.SHOW_STRATEGIES:
                if (messageParts.length != 2) {
                    returnValue = "provide necessary parameters";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                symbol = messageParts[1];
                if (!BinanceInfo.isSymbolExists(symbol)) {
                    returnValue = "Wrong symbol";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                returnValue = input + " - processed";
                break;

            case RealTImeOperations.CLOSE_ALL_POSITIONS:

            case RealTImeOperations.CLOSE_PROGRAM:

                returnValue = input + " - processed";
                break;

            case RealTImeOperations.GET_OPEN_POSITIONS:

            case RealTImeOperations.GET_CURRENT_BALANCE:

            case RealTImeOperations.SHOW_ALL_STRATEGIES:
                break;

            case RealTImeOperations.ACTIVATE_STRATEGY:

            case RealTImeOperations.DEACTIVATE_STRATEGY:
                if (messageParts.length != 4) {
                    returnValue = "provide necessary parameters";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                symbol = messageParts[1];
                if (!BinanceInfo.isSymbolExists(symbol)) {
                    returnValue = "Wrong symbol";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                entryStrategy = stringToEntryStrategy(messageParts[2], symbol);
                if (entryStrategy == null) {
                    returnValue = "This strategy don't exists";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                interval = null;
                for (CandlestickInterval candlestickInterval : CandlestickInterval.values()) {
                    if (candlestickInterval.toString().equals(messageParts[3])) interval = candlestickInterval;
                }
                if (interval == null) {
                    returnValue = "Wrong interval";
                    operation = RealTImeOperations.UNKNOWN_OPERATION;
                    break;
                }
                break;

            case "help":
                returnValue = """
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
                         interval options: 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h ,8h, 12h, 1d, 3d, 1w, 1M""";
                break;

            default:
                operation = RealTImeOperations.UNKNOWN_OPERATION;
                returnValue = operation + " - wrong operation";
        }
        return returnValue;
    }

    private EntryStrategy stringToEntryStrategy(String strategyName, String symbol) {
        if (RSI.getName().equals(strategyName)) {
            return new RSIEntryStrategy(symbol);
        } else if (MACD.getName().equals(strategyName)) {
            return new MACDOverRSIEntryStrategy(symbol);
        } else {
            return null;
        }
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
}
