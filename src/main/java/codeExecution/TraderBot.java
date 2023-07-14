package codeExecution;

import data.Config;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
            String result = proccessMessage(update.getMessage().getText());
            if (result.isEmpty() || result.isBlank()) {
                result = update.getMessage().getText() + " processed successfully";
            }
            SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
            message.setChatId(update.getMessage().getChatId().toString());
            message.setText(result);
            try {
                execute(message); // Call method to send the message
            } catch (TelegramApiException e) {
                log.error(e.toString());
            }
        }
    }

    private String proccessMessage(String command) {
        InputMessage message = new InputMessage();
        String result = message.processCommand(command);
        if (realTimeCommandOperator.getCommandsAndOps().containsKey(message.getOperation())) {
            realTimeCommandOperator.getCommandsAndOps().get(message.getOperation()).run(message);
        }
        return result;
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
