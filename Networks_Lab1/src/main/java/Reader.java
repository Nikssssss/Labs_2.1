import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Reader {
    private final MulticastSocket socket;
    private final HashMap<AppID, Integer> apps;
    InetAddress groupAddress;
    byte[] buffer = new byte[100];

    public Reader(InetAddress groupAddress, HashMap<AppID, Integer> apps) throws IOException {
        socket = new MulticastSocket(4444);
        socket.joinGroup(groupAddress);
        this.apps = apps;
        this.groupAddress = groupAddress;
    }

    public boolean read() {
        boolean isAdded = false;
        try {
            socket.setSoTimeout(3000);
            AppID appID;
            while (true){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                appID = new AppID(packet.getAddress().getHostName(), packet.getPort());
                if (apps.containsKey(appID)){
                    apps.replace(appID, (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                }
                else {
                    apps.put(appID, (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                    isAdded = true;
                }
            }
        } catch (SocketTimeoutException ignored) {
            return isAdded;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public void close(){
        socket.close();
    }
}
