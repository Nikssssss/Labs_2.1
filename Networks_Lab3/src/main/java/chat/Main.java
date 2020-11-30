package chat;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    static Logger logger;
    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getClassLoader().getResourceAsStream("logger.properties"));
            logger = Logger.getLogger(Main.class.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ChatClient chatClient;
        try {
            if (args.length == 3) {
                chatClient = new ChatClient(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), "", 0);
                logger.info("ChatClient with no parent has been created");
            } else if (args.length == 5) {
                chatClient = new ChatClient(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3], Integer.parseInt(args[4]));
                logger.info("ChatClient with a parent " + args[3] + ":" + Integer.parseInt(args[4]) + " has been created");
            } else {
                logger.severe("Wrong number of arguments");
                return;
            }
        } catch (SocketException e){
            logger.severe("Socket hasn't been created");
            return;
        }
        Thread threadChatClient = new Thread(chatClient);
        threadChatClient.start();
        try {
            threadChatClient.join();
            logger.info("ChatClient has ended the work");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
