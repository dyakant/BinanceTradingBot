package strategies.macdOverRSIStrategies;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.*;
import com.binance.client.model.trade.Order;
import data.AccountBalance;
import data.Config;
import data.DataHolder;
import org.jetbrains.annotations.NotNull;
import positions.PositionHandler;
import singletonHelpers.RequestClient;
import singletonHelpers.TelegramMessenger;
import strategies.EntryStrategy;
import strategies.ExitStrategy;
import strategies.macdOverRSIStrategies.Long.*;
import strategies.macdOverRSIStrategies.Short.*;
import utils.Trailer;

import java.math.BigDecimal;
import java.util.ArrayList;

public class MACDOverRSIEntryStrategy implements EntryStrategy {
    public final String NAME = "macd";
    private final AccountBalance accountBalance;
    double takeProfitPercentage = MACDOverRSIConstants.TAKE_PROFIT_PERCENTAGE;
    private double stopLossPercentage = MACDOverRSIConstants.STOP_LOSS_PERCENTAGE;
    private int leverage = MACDOverRSIConstants.LEVERAGE;
    private double requestedBuyingAmount = MACDOverRSIConstants.BUYING_AMOUNT;
    private volatile boolean bought = false;

    public MACDOverRSIEntryStrategy() {
        accountBalance = AccountBalance.getAccountBalance();
    }

    @Override
    public synchronized PositionHandler run(DataHolder realTimeData, String symbol) {
        boolean notInPosition = accountBalance.getPosition(symbol).getPositionAmt().compareTo(BigDecimal.valueOf(Config.DOUBLE_ZERO)) == Config.ZERO;
        if (notInPosition) {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            boolean noOpenOrders = syncRequestClient.getOpenOrders(symbol).size() == Config.ZERO;
            if (noOpenOrders) {
                double currentPrice = realTimeData.getCurrentPrice();
                boolean currentPriceAboveSMA = realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex()) < currentPrice;
                if (currentPriceAboveSMA) {
                    boolean rule1 = realTimeData.crossed(DataHolder.IndicatorType.MACD_OVER_RSI, DataHolder.CrossType.UP, DataHolder.CandleType.CLOSE, Config.ZERO);
                    if (rule1) {
                        if (bought) return null;
                        return buyAndCreatePositionHandler(symbol, currentPrice, PositionSide.LONG);
                    } else {
                        boolean macdValueBelowZero = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) < Config.ZERO;
                        if (macdValueBelowZero && decliningPyramid(realTimeData, DecliningType.NEGATIVE)) {
                            if (bought) return null;
                            return buyAndCreatePositionHandler(symbol, currentPrice, PositionSide.LONG);
                        }
                    }
                    bought = false;
                } else {
                    boolean rule1 = realTimeData.crossed(DataHolder.IndicatorType.MACD_OVER_RSI, DataHolder.CrossType.DOWN, DataHolder.CandleType.CLOSE, Config.ZERO);
                    if (rule1) {
                        if (bought) return null;
                        return buyAndCreatePositionHandler(symbol, currentPrice, PositionSide.SHORT);
                    } else {
                        if (realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) > Config.ZERO && decliningPyramid(realTimeData, DecliningType.POSITIVE)) {
                            if (bought) return null;
                            return buyAndCreatePositionHandler(symbol, currentPrice, PositionSide.SHORT);
                        }
                    }
                    bought = false;
                }
            }
        }
        return null;
    }

    private PositionHandler buyAndCreatePositionHandler(String symbol, Double currentPrice, PositionSide positionSide) {
        bought = true;
        PositionHandler positionHandler = null;
        try {
            Order buyOrder;
            ArrayList<ExitStrategy> exitStrategies;
            String buyingQty = utils.Utils.getBuyingQtyAsString(currentPrice, symbol, leverage, requestedBuyingAmount);
            TelegramMessenger.send(symbol, "buying " + positionSide.toString().toLowerCase() + ": " + buyingQty + ", price " + currentPrice);
            if (positionSide == PositionSide.LONG) {
                buyOrder = postOrder(OrderSide.BUY, symbol, currentPrice, buyingQty);
                exitStrategies = defineLongExitStrategy(currentPrice);
            } else {
                buyOrder = postOrder(OrderSide.SELL, symbol, currentPrice, buyingQty);
                exitStrategies = defineShortExitStrategy(currentPrice);
            }
            positionHandler = new PositionHandler(buyOrder, exitStrategies);
            TelegramMessenger.send(symbol, "buyOrder: " + buyOrder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return positionHandler;
    }

    private Order postOrder(OrderSide orderSide, String symbol, Double currentPrice, String buyingQty) {
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        syncRequestClient.changeInitialLeverage(symbol, leverage);
        return syncRequestClient.postOrder(
                symbol,
                orderSide,
                null,
                OrderType.LIMIT,
                TimeInForce.GTC,
                buyingQty,
                currentPrice.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                WorkingType.MARK_PRICE,
                null,
                NewOrderRespType.RESULT);
    }

    @NotNull
    private ArrayList<ExitStrategy> defineLongExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSILongExitStrategy1());
        exitStrategies.add(new MACDOverRSILongExitStrategy2());
        exitStrategies.add(new MACDOverRSILongExitStrategy3(new Trailer(currentPrice, MACDOverRSIConstants.POSITIVE_TRAILING_PERCENTAGE, PositionSide.LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy4(new Trailer(currentPrice, MACDOverRSIConstants.POSITIVE_TRAILING_PERCENTAGE, PositionSide.LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy5(new Trailer(currentPrice, MACDOverRSIConstants.CONSTANT_TRAILING_PERCENTAGE, PositionSide.LONG)));
        return exitStrategies;
    }

    @NotNull
    private ArrayList<ExitStrategy> defineShortExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSIShortExitStrategy1());
        exitStrategies.add(new MACDOverRSIShortExitStrategy2());
        exitStrategies.add(new MACDOverRSIShortExitStrategy3(new Trailer(currentPrice, MACDOverRSIConstants.POSITIVE_TRAILING_PERCENTAGE, PositionSide.SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy4(new Trailer(currentPrice, MACDOverRSIConstants.POSITIVE_TRAILING_PERCENTAGE, PositionSide.SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy5(new Trailer(currentPrice, MACDOverRSIConstants.CONSTANT_TRAILING_PERCENTAGE, PositionSide.SHORT)));
        return exitStrategies;
    }

    @Override
    public void setTakeProfitPercentage(double takeProfitPercentage) {
        this.takeProfitPercentage = takeProfitPercentage;
    }

    @Override
    public void setStopLossPercentage(double stopLossPercentage) {
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    @Override
    public void setRequestedBuyingAmount(double requestedBuyingAmount) {
        this.requestedBuyingAmount = requestedBuyingAmount;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public boolean decliningPyramid(DataHolder realTimeData, DecliningType type) {
        boolean rule1;
        boolean rule2;
        double currentMacdOverRsiValue = realTimeData.getMacdOverRsiCloseValue();
        double prevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 2);
        double prevPrevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 3);
        if (type == DecliningType.NEGATIVE) {
            rule1 = currentMacdOverRsiValue > prevMacdOverRsiValue;
            rule2 = prevMacdOverRsiValue > prevPrevMacdOverRsiValue;
        } else {
            rule1 = currentMacdOverRsiValue < prevMacdOverRsiValue;
            rule2 = prevMacdOverRsiValue < prevPrevMacdOverRsiValue;
        }
        return rule1 && rule2;
    }

    public enum DecliningType {
        NEGATIVE,
        POSITIVE
    }
}
