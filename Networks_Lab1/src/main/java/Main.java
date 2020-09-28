import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        InetAddress groupAddress;
        try {
            groupAddress = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("Invalid IP-address");
            return;
        }
        App app = new App(groupAddress);
        app.start();
    }
}
