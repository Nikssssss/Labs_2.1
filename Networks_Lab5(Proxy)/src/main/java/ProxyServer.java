import java.io.IOException;

public class ProxyServer {
    private SelectionReceiver selectionReceiver;

    public ProxyServer(int port) throws IOException {
        selectionReceiver = new SelectionReceiver(port);
    }

    public void start() throws IOException {
        selectionReceiver.start();
    }
}
