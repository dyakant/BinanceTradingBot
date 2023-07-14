package codeExecution;

import data.Config;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Anton Dyakov on 14.07.2023
 */
@Slf4j
public class TraderBot extends TelegramLongPollingBot {
    private static final String apiToken = Config.TELEGRAM_API_TOKEN;
    private final RealTimeCommandOperator realTimeCommandOperator;

    public TraderBot() {
        super(apiToken);
        this.realTimeCommandOperator = new RealTimeCommandOperator();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var list = update.getMessage().getText().split(",");
            String result;
            if (list.length <= 1) {
                result = proccessOneMessage(list[0]);
            } else {
                result = processBatchMessages(list);
            }
            sendMessageToChat(result, update.getMessage().getChatId().toString());
        }
    }

    private String proccessOneMessage(String command) {
        String result = proccessMessage(command);
        if (result.isEmpty() || result.isBlank()) {
            result = "[" + command + "] processed successfully";
        }
        return result;
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
        if (realTimeCommandOperator.getCommandsAndOps().containsKey(message.getOperation())) {
            realTimeCommandOperator.getCommandsAndOps().get(message.getOperation()).run(message);
        }
        return result;
    }

    private void sendMessageToChat(String result, String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
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

    public void run() {

    }
}
