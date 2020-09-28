import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class App {
    private Reader reader;
    private Writer writer;
    private final HashMap<AppID, Integer> apps;
    private final int maxTimeDifference = 6;
    private boolean isWorking = true;
    private Thread thread = Thread.currentThread();

    public App(InetAddress groupAddress) {
        apps = new HashMap<>();
        try {
            reader = new Reader(groupAddress, apps);
            writer = new Writer(groupAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                isWorking = false;
                System.out.println("Check CTRL-C");
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void start() {
        String message = "Hello, world!";
        boolean isAdded, isRemoved;
        while (isWorking) {
            writer.write(message);
            isAdded = reader.read();
            isRemoved = checkRemoving();
            if (isAdded || isRemoved) {
                printApps();
            }
        }
        reader.close();
        System.out.println("Reader closed");
        writer.close();
        System.out.println("Writer closed");
    }

    private boolean checkRemoving() {
        return apps.values().removeIf(x -> (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - x > maxTimeDifference));
    }

    private void printApps() {
        for (Map.Entry<AppID, Integer> entry : apps.entrySet()) {
            System.out.println(entry.getKey().getID());
        }
        System.out.println();
    }
}
