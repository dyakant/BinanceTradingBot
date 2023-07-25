package com.btb.singletonHelpers;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class TelegramMessenger {
    private static final TelegramBot telegramBot = TelegramBot.getTelegramBot();

    public static void send(String symbol, String text) {
        sendToTelegram("*" + symbol.toUpperCase() + "* " + getEscapedText(text));
    }

    public static void send(String text) {
        sendToTelegram(getEscapedText(text));
    }

    private static synchronized void sendToTelegram(String text) {
        telegramBot.sendMessageToChat(prepareMessage(text));
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
