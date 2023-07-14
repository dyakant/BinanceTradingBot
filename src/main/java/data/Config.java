package data;

import codeExecution.TraderBot;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class Config {
    //Personal information:
    public static String API_KEY = "<Your binance api key>";
    public static String SECRET_KEY = "<Your binance secret key>";
    public static String TELEGRAM_API_TOKEN = "<Your telegram bot api token>";
    public static String TELEGRAM_CHAT_ID = "<Your telegram group chat id>";
    public static int THREAD_NUM = 6;
    public static int CANDLE_NUM = 150;
    public static String COMMAND_CONSOLE = "console";
    public static String COMMAND_BOT = "bot";
    public static String COMMAND = COMMAND_CONSOLE;
    public static final double DOUBLE_ZERO = 0.0;
    public static final String NEW = "NEW";
    public static final String PARTIALLY_FILLED = "PARTIALLY_FILLED";
    public static final String FILLED = "FILLED";
    public static final String CANCELED = "CANCELED";
    public static final String EXPIRED = "EXPIRED";
    public static final int ZERO = 0;
    public static final String REDUCE_ONLY = "true";

    public Config() {
        printIntroLog();
        try {
            setPropValues();
            registerTelegramBot();
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public void setPropValues() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Config.properties")) {
            Properties prop = new Properties();
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file 'Config.properties' not found in the classpath");
            }
            API_KEY = prop.getProperty("API_KEY");
            SECRET_KEY = prop.getProperty("SECRET_KEY");
            TELEGRAM_API_TOKEN = prop.getProperty("TELEGRAM_API_TOKEN");
            TELEGRAM_CHAT_ID = prop.getProperty("TELEGRAM_CHAT_ID");
            THREAD_NUM = Integer.parseInt(prop.getProperty("THREAD_NUM"));
            CANDLE_NUM = Integer.parseInt(prop.getProperty("CANDLE_NUM"));
            COMMAND = prop.getProperty("COMMAND");
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    private void registerTelegramBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TraderBot());
        } catch (TelegramApiException e) {
            log.error(e.toString());
        }
    }

    private void printIntroLog() {
        log.info("""
                  
                  *********************************************************************
                  _______ _____            _____  ______ _____    ____   ____ _______\s
                 |__   __|  __ \\     /\\   |  __ \\|  ____|  __ \\  |  _ \\ / __ \\__   __|
                    | |  | |__) |   /  \\  | |  | | |__  | |__) | | |_) | |  | | | |  \s
                    | |  |  _  /   / /\\ \\ | |  | |  __| |  _  /  |  _ <| |  | | | |  \s
                    | |  | | \\ \\  / ____ \\| |__| | |____| | \\ \\  | |_) | |__| | | |  \s
                    |_|  |_|  \\_\\/_/    \\_\\_____/|______|_|  \\_\\ |____/ \\____/  |_|  \s
                                                                                     \s
                 *********************************************************************                                                                  \s
                """);
    }
}