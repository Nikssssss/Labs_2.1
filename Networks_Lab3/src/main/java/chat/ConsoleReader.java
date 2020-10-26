package chat;

import observers.Observable;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;

public class ConsoleReader extends Observable implements Runnable {
    static Logger logger;
    private final int port;
    private final String clientName;

    public ConsoleReader(int port, String clientName){
        logger = Logger.getLogger(this.getClass().getName());
        this.port = port;
        this.clientName = clientName;
    }

    @Override
    public void run() {
        String textMessage;
        InetSocketAddress localHost = new InetSocketAddress("127.0.0.1", port);
        Scanner scanner = new Scanner(System.in);
        while (!(textMessage = scanner.nextLine()).equals("/END")) {
            System.out.println(clientName + ": " + textMessage);
            notifyObservers(new Message(MessageType.CONSOLE_REQUEST, UUID.randomUUID(), textMessage, clientName), localHost);
        }
    }
}
