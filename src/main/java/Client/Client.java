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

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                        System.out.println("Socket closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendFile() {
        try {
            socket = new Socket(ip, port);
            DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());

            socketOutputStream.writeInt(file.getName().length());
            socketOutputStream.writeUTF(file.getName());
            if (socketInputStream.readBoolean()) {
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

                if (!socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Socket " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " closed");
            }
        } catch (IOException e){
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
