import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Writer {
    DatagramSocket socket;
    InetAddress groupAddress;

    public Writer(InetAddress groupAddress) throws SocketException {
        socket = new DatagramSocket();
        this.groupAddress = groupAddress;
    }

    public void write(String message) {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, 4444);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        socket.close();
    }
}
