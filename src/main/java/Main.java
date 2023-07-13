import codeExecution.RealTimeCommandOperator;
import data.AccountBalance;
import data.Config;
import singletonHelpers.BinanceInfo;
import singletonHelpers.TelegramMessenger;

public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        AccountBalance accountBalance = AccountBalance.getAccountBalance();
        BinanceInfo binanceInfo = BinanceInfo.getBinanceInfo();
        RealTimeCommandOperator realTimeCommandOperator = new RealTimeCommandOperator();
        TelegramMessenger.sendToTelegram("Binance Trading Bot is running...");
        System.out.println("Binance Trading Bot is running! Type commands.");
        try {
            realTimeCommandOperator.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


