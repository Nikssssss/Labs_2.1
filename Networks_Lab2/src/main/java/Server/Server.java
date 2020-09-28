package Server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {
    private final Integer port;
    private ServerSocket serverSocket;
    private final File uploads;

    public Server(int port) {
        this.port = port;
        uploads = new File(System.getProperty("user.dir") + "\\uploads");
        if (!uploads.exists()){
            if (!uploads.mkdir()){
                System.out.println("Failed to create directory");
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    serverSocket.close();
                    System.out.println("Server Socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " connected");
                ClientHandler clientHandler = new ClientHandler(clientSocket, uploads);
                Thread handler = new Thread(clientHandler);
                handler.start();
            }
        } catch (SocketException ignored){
        } catch (IOException e){
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
