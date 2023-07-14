import codeExecution.ConsoleCommand;
import data.AccountBalance;
import data.Config;
import lombok.extern.slf4j.Slf4j;
import singletonHelpers.BinanceInfo;
import singletonHelpers.TelegramMessenger;

import static data.Config.COMMAND;
import static data.Config.COMMAND_CONSOLE;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        AccountBalance accountBalance = AccountBalance.getAccountBalance();
        BinanceInfo binanceInfo = BinanceInfo.getBinanceInfo();
        TelegramMessenger.send("Binance Trading Bot is running... Balance: " + AccountBalance.getAccountBalance().getCoinBalance("usdt"));
        log.info("Binance Trading Bot is running! Type commands.");
        if (COMMAND.equals(COMMAND_CONSOLE)) {
            runConsoleCommand();
        }
    }

    private static void runConsoleCommand() {
        System.out.println("Console command terminal is started.");
        ConsoleCommand consoleCommand = new ConsoleCommand();
        try {
            consoleCommand.run();
        } catch (InterruptedException e) {
            log.error(e.toString());
        }
    }
}


