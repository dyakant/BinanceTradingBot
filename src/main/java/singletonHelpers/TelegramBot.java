package singletonHelpers;

import codeExecution.InputMessage;
import codeExecution.RealTimeCommandOperator;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

import static data.Config.TELEGRAM_API_TOKEN;
import static data.Config.TELEGRAM_CHAT_ID;

/**
 * Telegram bot is connected with the chat
 * It receives messages as commands and processes them.
 * Also, it's used as singleton to send messages
 *
 * Created by Anton Dyakov on 17.07.2023
 */
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final RealTimeCommandOperator realTimeCommandOperator;

    private static class TelegramBotHolder {
        private static final TelegramBot telegramBot = new TelegramBot();
    }

    public static TelegramBot getTelegramBot() {
        return TelegramBotHolder.telegramBot;
    }

    private TelegramBot() {
        super(TELEGRAM_API_TOKEN);
        this.realTimeCommandOperator = new RealTimeCommandOperator();
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            log.error("Error during telegram bot registering: ", e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (chatIdIsWrong(update.getMessage().getChatId().toString())) return;
            var list = update.getMessage().getText().split(",");
            String result;
            if (list.length <= 1) {
                result = proccessOneMessage(list[0]);
            } else {
                result = processBatchMessages(list);
            }
            TelegramMessenger.send(result);
//            sendMessageToChat(result);
        }
    }

    private boolean chatIdIsWrong(String chatId) {
        return (!TELEGRAM_CHAT_ID.equals(chatId));
    }

    private String proccessOneMessage(String command) {
        return proccessMessage(command);
    }

    private String processBatchMessages(String[] commands) {
        List<String> list = new ArrayList<>();
        for (String command : commands) {
            list.add(proccessOneMessage(command));
        }
        return String.join("\n", list);
    }

    private String proccessMessage(String command) {
        InputMessage message = new InputMessage();
        String result = message.processCommand(command);
        if (realTimeCommandOperator.getCommandsAndOps().containsKey(message.getOperation())) { // TODO; move to InputMessage
            realTimeCommandOperator.getCommandsAndOps().get(message.getOperation()).run(message);
        }
        return result;
    }

    public void sendMessageToChat(String result) {
        SendMessage message = new SendMessage();
        message.setChatId(TELEGRAM_CHAT_ID);
        message.enableMarkdownV2(true);
        message.setText(result);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.toString());
        }
    }

    @Override
    public String getBotUsername() {
        return "TraderBot";
    }

    @Override
    public void onRegister() {
        super.onRegister();
        log.info("Telegram bot is registered");
    }
}
