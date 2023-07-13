package singletonHelpers;

import data.Config;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TelegramMessenger {
    private static final String apiToken = Config.TELEGRAM_API_TOKEN;
    private static final String chatId = Config.TELEGRAM_CHAT_ID;
    private static final String PARSE_MODE = "MarkdownV2";

    public static void send(String symbol, String text) {
        sendToTelegram("*" + symbol.toUpperCase() + "* " + text.replaceAll("[\\W]", "\\\\$0"));
    }

    public static void send(String text) {
        sendToTelegram(text.replaceAll("[\\W]", "\\\\$0"));
    }

    private static synchronized void sendToTelegram(String text) {
        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=%s";
        urlString = String.format(urlString, apiToken, chatId, prepareMessage(text), PARSE_MODE);
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
        } catch (Exception ex) {
            System.out.println("Exception while sending message to Telegram: " + ex);
        }
    }

    @NotNull
    private static String prepareMessage(String text) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm:ss")).replaceAll("[\\W]", "\\\\$0");
        return "_\\[" + currentTime + "\\]_ " + text;
    }
}
