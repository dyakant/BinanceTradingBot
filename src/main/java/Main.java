import codeExecution.RealTimeCommandOperator;
import data.AccountBalance;
import data.Config;
import lombok.extern.slf4j.Slf4j;
import singletonHelpers.BinanceInfo;
import singletonHelpers.TelegramMessenger;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        AccountBalance accountBalance = AccountBalance.getAccountBalance();
        BinanceInfo binanceInfo = BinanceInfo.getBinanceInfo();
        RealTimeCommandOperator realTimeCommandOperator = new RealTimeCommandOperator();
        TelegramMessenger.send("Binance Trading Bot is running... Balance: " + AccountBalance.getAccountBalance().getCoinBalance("usdt"));
        log.info("Binance Trading Bot is running! Type commands.");
        try {
            realTimeCommandOperator.run();
        } catch (InterruptedException e) {
            log.error(e.toString());
        }
    }
}


