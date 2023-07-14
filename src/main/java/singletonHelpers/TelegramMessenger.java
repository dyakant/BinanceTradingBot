package singletonHelpers;

import data.Config;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Slf4j
public class TelegramMessenger {
    private static final String apiToken = Config.TELEGRAM_API_TOKEN;
    private static final String chatId = Config.TELEGRAM_CHAT_ID;
    private static final String PARSE_MODE = "MarkdownV2";

    public static void send(String symbol, String text) {
        sendToTelegram("*" + symbol.toUpperCase() + "* " + getEscapedText(text));
    }

    public static void send(String text) {
        sendToTelegram(getEscapedText(text));
    }

    private static synchronized void sendToTelegram(String text) {
        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=%s";
        urlString = String.format(urlString, apiToken, chatId, prepareMessage(text), PARSE_MODE);
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    @NotNull
    private static String prepareMessage(String text) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
        return "_\\[" + getEscapedText(currentTime) + "\\]_ " + text;
    }

    private static String getEscapedText(String text) {
        Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\]_~`>#-=!.+*?^$\\\\|]");
        return SPECIAL_REGEX_CHARS.matcher(text).replaceAll("\\\\$0");
    }
}
