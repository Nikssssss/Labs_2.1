package Client;

import java.io.*;
import java.net.Socket;

public class Client {
    private File file;
    private String ip;
    private Integer port;
    private Socket socket;

    public Client(String filePath, String ip, int port){
        file = new File(filePath);
        this.ip = ip;
        this.port = port;
    }

    public void sendFile() {
        try {
            socket = new Socket(ip, port);
            DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());

            socketOutputStream.writeUTF(file.getName());
            socketOutputStream.writeLong(file.length());
            socketOutputStream.flush();

            byte[] buffer = new byte[2048];
            FileInputStream fileInputStream = new FileInputStream(file);
            int writeCount;
            while ((writeCount = fileInputStream.read(buffer)) > 0) {
                socketOutputStream.write(buffer, 0, writeCount);
                socketOutputStream.flush();
            }

            if (socketInputStream.readBoolean()) {
                System.out.println("File was transferred successfully");
            } else {
                System.out.println("File transferring was failed");
            }

            socket.close();
            System.out.println("socket " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " closed");
        } catch (IOException e){
            try {
                socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
