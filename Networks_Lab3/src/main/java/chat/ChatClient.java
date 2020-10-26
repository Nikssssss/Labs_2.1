package chat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class ChatClient implements Runnable{
    static Logger logger;
    private final String clientName;
    private final ArrayList<InetSocketAddress> neighborNodes;
    private final Sender sender;
    private final Receiver receiver;
    private final ConsoleReader consoleReader;
    private final MessageRecorder messageRecorder;
    private final AckChecker ackChecker;
    private final MessageHandler messageHandler;
    private final DatagramSocket socket;
    private Thread threadConsoleReader;
    private Thread threadReceiver;
    private Thread threadAckChecker;
    private boolean isActive;

    public ChatClient(String clientName, int loss, int port, String parentIP, int parentPort) throws SocketException {
        logger = Logger.getLogger(this.getClass().getName());
        isActive = true;
        this.clientName = clientName;
        socket = new DatagramSocket(port);
        neighborNodes = new ArrayList<>();
        if (!parentIP.equals("")) {
            neighborNodes.add(new InetSocketAddress(parentIP, parentPort));
        }
        messageRecorder = new MessageRecorder();
        ackChecker = new AckChecker(messageRecorder, port);
        sender = new Sender(neighborNodes, messageRecorder, ackChecker, socket);
        ackChecker.addObserver(sender);
        consoleReader = new ConsoleReader(port, clientName);
        consoleReader.addObserver(sender);
        messageHandler = new MessageHandler(neighborNodes, messageRecorder);
        messageHandler.addObserver(sender);
        receiver = new Receiver(loss, socket, messageHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isActive) {
                sendSubstitute();
                close();
            }
        }));
    }

    @Override
    public void run() {
        threadConsoleReader = new Thread(consoleReader);
        threadConsoleReader.start();
        threadReceiver = new Thread(receiver);
        threadReceiver.start();
        threadAckChecker = new Thread(ackChecker);
        threadAckChecker.start();
        try {
            threadConsoleReader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendSubstitute();
        close();
        isActive = false;
    }

    private void sendSubstitute(){
        InetSocketAddress substitute = neighborNodes.get(0);
        try {
            sender.send(new Message(MessageType.SUBSTITUTE_REQUEST, UUID.randomUUID(), substitute.getAddress() + ":" + substitute.getPort(), clientName), substitute);
            sender.send(new Message(MessageType.DELETE_REQUEST, UUID.randomUUID(), "", clientName), substitute);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (messageRecorder.hasMessageType(MessageType.SUBSTITUTE_REQUEST)){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("Substitute has been sent");
    }

    private void close(){
        socket.close();
        threadReceiver.interrupt();
        threadAckChecker.interrupt();
        try {
            threadReceiver.join();
            threadAckChecker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Resources has been cleaned");
    }
}
