package Server;

public class ServerMain {
    public static void main(String[] args) {
        Server server = new Server(2048);
        server.start();
    }
}
