import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ProxyServer proxyServer;
        try {
            proxyServer = new ProxyServer(1080);
            proxyServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
