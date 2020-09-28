package Client;

public class ClientMain {
    public static void main(String[] args) {
        String filePath = System.getProperty("user.dir") + "\\" + "videoplayback.mp4";
        Client client = new Client(filePath, "localhost", 2048);
        client.sendFile();
    }
}
