package codeExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by Anton Dyakov on 14.07.2023
 */
@Slf4j
public class ConsoleCommand {
    private final ConcurrentLinkedDeque<InputMessage> awaitingMessages = new ConcurrentLinkedDeque<>();
    private final RealTimeCommandOperator realTimeCommandOperator;
    private boolean shouldTerminate = false;

    public ConsoleCommand() {
        this.realTimeCommandOperator = new RealTimeCommandOperator();
    }

    public void run() throws InterruptedException {
        Thread realTimeCommandOperatorThread = new Thread(new KeyboardReader());
        realTimeCommandOperatorThread.start();
        synchronized (awaitingMessages) {
            while (!shouldTerminate) {
                InputMessage message = awaitingMessages.poll();
                if (message == null) {
                    awaitingMessages.wait();
                } else {
                    if (realTimeCommandOperator.getCommandsAndOps().containsKey(message.getOperation())) {
                        realTimeCommandOperator.getCommandsAndOps().get(message.getOperation()).run(message);
                    }
                }
            }
        }
    }

    private class KeyboardReader implements Runnable {
        public void run() {
            Scanner scan = new Scanner(System.in);
            while (true) {
                try {
                    InputMessage message = new InputMessage();
                    System.out.print("# ");
                    String input = scan.nextLine();
                    String result = message.processCommand(input);
                    System.out.println(result);
                    String messageOperation = message.getOperation();
                    if (!messageOperation.equals(RealTImeOperations.UNKNOWN_OPERATION)) {
                        synchronized (awaitingMessages) {
                            awaitingMessages.add(message);
                            awaitingMessages.notifyAll();
                        }
                        if (messageOperation.equals(RealTImeOperations.CLOSE_PROGRAM)) break;
                    }
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
        }
    }
}
