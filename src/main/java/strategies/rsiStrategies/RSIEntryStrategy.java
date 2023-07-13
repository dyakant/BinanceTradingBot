package strategies.rsiStrategies;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.enums.TimeInForce;
import com.binance.client.model.trade.Order;
import data.DataHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import positions.PositionHandler;
import singletonHelpers.RequestClient;
import singletonHelpers.TelegramMessenger;
import strategies.EntryStrategy;
import strategies.ExitStrategy;
import strategies.PositionInStrategy;
import utils.Utils;

import java.util.ArrayList;

import static com.binance.client.model.enums.NewOrderRespType.RESULT;
import static com.binance.client.model.enums.OrderSide.BUY;
import static com.binance.client.model.enums.OrderSide.SELL;
import static com.binance.client.model.enums.OrderType.*;
import static com.binance.client.model.enums.WorkingType.MARK_PRICE;
import static data.DataHolder.CandleType.CLOSE;
import static data.DataHolder.CrossType.DOWN;
import static data.DataHolder.CrossType.UP;
import static data.DataHolder.IndicatorType.RSI;
import static strategies.PositionInStrategy.*;
import static strategies.rsiStrategies.RSIConstants.*;

@Slf4j
public class RSIEntryStrategy implements EntryStrategy {
    public final String NAME = "rsi";
    double takeProfitPercentage = TAKE_PROFIT_PERCENTAGE;
    private double stopLossPercentage = STOP_LOSS_PERCENTAGE;
    private int leverage = LEVERAGE;
    private double requestedBuyingAmount = BUYING_AMOUNT;
    private PositionInStrategy positionInStrategy = POSITION_ONE;
    private int time_passed_from_position_2 = 0;
    double rsiValueToCheckForPosition3 = -1;

    public synchronized PositionHandler run(DataHolder realTimeData, String symbol) {
        if (positionInStrategy == POSITION_ONE) {
            if (realTimeData.crossed(RSI, DOWN, CLOSE, RSI_ENTRY_THRESHOLD_1)) {
                positionInStrategy = POSITION_TWO;
            }
            return null;
        } else if (positionInStrategy == POSITION_TWO) {
            if (realTimeData.crossed(RSI, UP, CLOSE, RSI_ENTRY_THRESHOLD_2)) {
                rsiValueToCheckForPosition3 = realTimeData.getRsiCloseValue();
                positionInStrategy = POSITION_THREE;
            }
            return null;
        } else if (positionInStrategy == POSITION_THREE) {
            if (time_passed_from_position_2 >= 2) {
                time_passed_from_position_2 = 0;
                rsiValueToCheckForPosition3 = -1;
                positionInStrategy = POSITION_TWO;
                return null;
            }
            if (rsiValueToCheckForPosition3 != realTimeData.getRsiCloseValue()) {
                time_passed_from_position_2++;
            }
            if (realTimeData.above(RSI, CLOSE, RSI_ENTRY_THRESHOLD_3)) {
                time_passed_from_position_2 = 0;
                positionInStrategy = POSITION_ONE;
                rsiValueToCheckForPosition3 = -1;

                double currentPrice = realTimeData.getCurrentPrice();
                String buyingQty = Utils.getBuyingQtyAsString(currentPrice, symbol, leverage, requestedBuyingAmount);
                try {
                    TelegramMessenger.send(symbol, "buying long... " + buyingQty + ", price " + currentPrice);
                    Order buyOrder = postOrder(symbol, BUY, MARKET, buyingQty, null, null);
                    log.info("{}. buy order: {}", symbol, buyOrder);

                    String takeProfitPrice = Utils.getTakeProfitPriceAsString(realTimeData, symbol, takeProfitPercentage);
                    postOrder(symbol, SELL, TAKE_PROFIT, buyingQty, takeProfitPrice, takeProfitPrice);

                    String stopLossPrice = Utils.getStopLossPriceAsString(realTimeData, symbol, stopLossPercentage);
                    postOrder(symbol, SELL, STOP, buyingQty, stopLossPrice, stopLossPrice);

                    ArrayList<ExitStrategy> exitStrategies = defineExitStrategy();
                    return new PositionHandler(buyOrder, exitStrategies);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private Order postOrder(String symbol, OrderSide orderSide, OrderType orderType, String buyingQty, String price, String stopPrice) {
        SyncRequestClient syncRequestClient = RequestClient.getRequestClient().getSyncRequestClient();
        syncRequestClient.changeInitialLeverage(symbol, leverage);
        return syncRequestClient.postOrder(
                symbol,
                orderSide,
                null,
                orderType,
                TimeInForce.GTC,
                buyingQty,
                price,
                null,
                null,
                stopPrice,
                null,
                null,
                null,
                MARK_PRICE,
                null,
                RESULT);
    }

    @NotNull
    private ArrayList<ExitStrategy> defineExitStrategy() {
        ArrayList<ExitStrategy> exitStrategies = new ArrayList<>();
        exitStrategies.add(new RSIExitStrategy1());
        exitStrategies.add(new RSIExitStrategy2());
        exitStrategies.add(new RSIExitStrategy3());
        exitStrategies.add(new RSIExitStrategy4());
        return exitStrategies;
    }

    public void setTakeProfitPercentage(double takeProfitPercentage) {
        this.takeProfitPercentage = takeProfitPercentage;
    }

    public void setStopLossPercentage(double stopLossPercentage) {
        this.stopLossPercentage = stopLossPercentage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    public void setRequestedBuyingAmount(double requestedBuyingAmount) {
        this.requestedBuyingAmount = requestedBuyingAmount;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
