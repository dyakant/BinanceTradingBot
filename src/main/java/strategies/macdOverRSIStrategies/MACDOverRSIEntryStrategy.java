package strategies.macdOverRSIStrategies;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.PositionSide;
import com.binance.client.model.trade.Order;
import data.AccountBalance;
import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
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

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.LIMIT;
import static com.binance.client.model.enums.PositionSide.LONG;
import static com.binance.client.model.enums.PositionSide.SHORT;
import static com.binance.client.model.enums.TimeInForce.GTC;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static data.Config.DOUBLE_ZERO;
import static data.Config.ZERO;
import static data.DataHolder.CandleType.CLOSE;
import static data.DataHolder.CrossType.DOWN;
import static data.DataHolder.CrossType.UP;
import static data.DataHolder.IndicatorType.MACD_OVER_RSI;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.CONSTANT_TRAILING_PERCENTAGE;
import static strategies.macdOverRSIStrategies.MACDOverRSIConstants.POSITIVE_TRAILING_PERCENTAGE;
import static strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.NEGATIVE;
import static strategies.macdOverRSIStrategies.MACDOverRSIEntryStrategy.DecliningType.POSITIVE;

@Slf4j
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
        boolean notInPosition = accountBalance.getPosition(symbol).getPositionAmt().compareTo(BigDecimal.valueOf(DOUBLE_ZERO)) == ZERO;
        if (notInPosition) {
            SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
            boolean noOpenOrders = syncRequestClient.getOpenOrders(symbol).size() == ZERO;
            if (noOpenOrders) {
                double currentPrice = realTimeData.getCurrentPrice();
                boolean currentPriceAboveSMA = realTimeData.getSMAValueAtIndex(realTimeData.getLastIndex()) < currentPrice;
                if (currentPriceAboveSMA) {
                    boolean rule1 = realTimeData.crossed(MACD_OVER_RSI, UP, CLOSE, ZERO);
                    if (rule1) {
                        if (bought) return null;
                        return buyAndCreatePositionHandler(symbol, currentPrice, LONG);
                    } else {
                        boolean macdValueBelowZero = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) < ZERO;
                        if (macdValueBelowZero && decliningPyramid(realTimeData, NEGATIVE)) {
                            if (bought) return null;
                            return buyAndCreatePositionHandler(symbol, currentPrice, LONG);
                        }
                    }
                    bought = false;
                } else {
                    boolean rule1 = realTimeData.crossed(MACD_OVER_RSI, DOWN, CLOSE, ZERO);
                    if (rule1) {
                        if (bought) return null;
                        return buyAndCreatePositionHandler(symbol, currentPrice, SHORT);
                    } else {
                        if (realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex()) > ZERO
                                && decliningPyramid(realTimeData, POSITIVE)) {
                            if (bought) return null;
                            return buyAndCreatePositionHandler(symbol, currentPrice, SHORT);
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
            if (positionSide == LONG) {
                buyOrder = postOrder(symbol, BUY, currentPrice, buyingQty);
                exitStrategies = defineLongExitStrategy(currentPrice);
            } else {
                buyOrder = postOrder(symbol, SELL, currentPrice, buyingQty);
                exitStrategies = defineShortExitStrategy(currentPrice);
            }
            positionHandler = new PositionHandler(buyOrder, exitStrategies);
            TelegramMessenger.send(symbol, "executed");
            log.info("{}, buyOrder: {}", symbol, buyOrder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return positionHandler;
    }

    private Order postOrder(String symbol, OrderSide orderSide, Double currentPrice, String buyingQty) {
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        syncRequestClient.changeInitialLeverage(symbol, leverage);
        return syncRequestClient.postOrder(
                symbol,
                orderSide,
                null,
                LIMIT,
                GTC,
                buyingQty,
                currentPrice.toString(),
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

    @NotNull
    private ArrayList<ExitStrategy> defineLongExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSILongExitStrategy1());
        exitStrategies.add(new MACDOverRSILongExitStrategy2());
        exitStrategies.add(new MACDOverRSILongExitStrategy3(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy4(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, LONG)));
        exitStrategies.add(new MACDOverRSILongExitStrategy5(new Trailer(currentPrice, CONSTANT_TRAILING_PERCENTAGE, LONG)));
        return exitStrategies;
    }

    @NotNull
    private ArrayList<ExitStrategy> defineShortExitStrategy(Double currentPrice) {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new MACDOverRSIShortExitStrategy1());
        exitStrategies.add(new MACDOverRSIShortExitStrategy2());
        exitStrategies.add(new MACDOverRSIShortExitStrategy3(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy4(new Trailer(currentPrice, POSITIVE_TRAILING_PERCENTAGE, SHORT)));
        exitStrategies.add(new MACDOverRSIShortExitStrategy5(new Trailer(currentPrice, CONSTANT_TRAILING_PERCENTAGE, SHORT)));
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

    private boolean decliningPyramid(DataHolder realTimeData, DecliningType type) {
        boolean rule1;
        boolean rule2;
        double currentMacdOverRsiValue = realTimeData.getMacdOverRsiCloseValue();
        double prevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 2);
        double prevPrevMacdOverRsiValue = realTimeData.getMacdOverRsiValueAtIndex(realTimeData.getLastIndex() - 3);
        if (type == NEGATIVE) {
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
