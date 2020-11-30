package chat;

import observers.Observable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;

public class AckChecker extends Observable implements Runnable{
    static Logger logger;
    private MessageRecorder messageRecorder;
    private final int timeout = 3000;
    private final int port;

    public AckChecker(MessageRecorder messageRecorder, int port) {
        logger = Logger.getLogger(this.getClass().getName());
        this.messageRecorder = messageRecorder;
        this.port = port;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            if (messageRecorder.isEmpty()){
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            for (Map.Entry<MessageID, Long> entry : messageRecorder.getOverdueMessages(timeout).entrySet()){
                notifyObservers(entry.getKey().getMessage(), new InetSocketAddress("127.0.0.1", port), entry.getKey().getAddress());
            }
            synchronized (this) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
