package com.btb;

import com.btb.codeExecution.ConsoleCommand;
import com.btb.data.AccountBalance;
import com.btb.data.Config;
import lombok.extern.slf4j.Slf4j;
import com.btb.singletonHelpers.BinanceInfo;
import com.btb.singletonHelpers.TelegramMessenger;

import static com.btb.data.Config.COMMAND;
import static com.btb.data.Config.COMMAND_CONSOLE;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        AccountBalance accountBalance = AccountBalance.getAccountBalance();
        BinanceInfo binanceInfo = BinanceInfo.getBinanceInfo();
        TelegramMessenger.send("Binance Trading Bot is running... \nBalance: " + accountBalance.getCoinBalance("usdt"));
        System.out.println("Binance Trading Bot is running!");
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


