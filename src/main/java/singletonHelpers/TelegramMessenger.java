package singletonHelpers;

import data.Config;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TelegramMessenger {
    private static final String apiToken = Config.TELEGRAM_API_TOKEN;
    private static final String chatId = Config.TELEGRAM_CHAT_ID;

    public static void send(String symbol, String text) {
        sendToTelegram("<b>" + symbol.toUpperCase() + "</b> " + text);
    }

    public static synchronized void sendToTelegram(String text) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd hh:mm:ss"));
        String message = "<i>[" + currentTime + "]</i> " + text;
        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=HTML";
        urlString = String.format(urlString, apiToken, chatId, message);

        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
        } catch (Exception ex) {
            System.out.println("Exception while sending message to Telegram: " + ex);
        }
    }
}
