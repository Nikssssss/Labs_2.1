package Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket socket;
    private final File uploads;
    private final long timeout = 3000000000L;

    public ClientHandler(Socket clientSocket, File uploads){
        this.socket = clientSocket;
        this.uploads = uploads;

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                        System.out.println("ClientHandler: socket closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void run() {
        try {
            System.out.println("Handler for " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " has started");
            DataInputStream socketInputStream =  new DataInputStream(socket.getInputStream());
            DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());

            int fileNameLength = socketInputStream.readInt();
            String fileName = socketInputStream.readUTF();
            if (fileNameLength != fileName.length()){
                socketOutputStream.writeBoolean(false);
            }
            else {
                socketOutputStream.writeBoolean(true);
                long fileLength = socketInputStream.readLong();
                File file = new File(uploads + "\\" + fileName);
                if (file.exists()) {
                    int i = 0;
                    while (file.exists()) {
                        file = new File(uploads + "\\" + "(" + i + ")" + fileName);
                        i++;
                    }
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] buffer = new byte[2048];
                int readCount;
                int numberOfBytes = 0;
                long timeStart, timeEnd, currentSpeed, averageSpeed = 0;
                long lastUpdateTime = System.nanoTime();
                while (numberOfBytes < fileLength) {
                    timeStart = System.nanoTime();
                    readCount = socketInputStream.read(buffer);
                    timeEnd = System.nanoTime();
                    fileOutputStream.write(buffer, 0, readCount);
                    if ((System.nanoTime() - lastUpdateTime > timeout) || numberOfBytes == 0) {
                        lastUpdateTime = System.nanoTime();
                        currentSpeed = calculateCurrentSpeed(timeEnd - timeStart, readCount);
                        if (averageSpeed == 0) {
                            averageSpeed = currentSpeed;
                        }
                        averageSpeed = (averageSpeed + currentSpeed) / 2;
                        System.out.print(socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                        System.out.println(" - current speed: " + currentSpeed + "MB/s, average speed: " + averageSpeed + "MB/s");
                    }
                    numberOfBytes += readCount;
                }

                socketOutputStream.writeBoolean(file.length() == fileLength);
                socketOutputStream.flush();

                System.out.println("Handler for " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " has ended");
                if (!socket.isClosed()){
                    socket.close();
                }
            }
        } catch (IOException e) {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e){
            if (!socket.isClosed()){
                try {
                    socket.close();
                    System.out.println("ClientHandler: socket closed");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private long calculateCurrentSpeed(long time, long numberOfBytes){
        double bytePerSec = (double)numberOfBytes / ((double)time / 1000000000);
        double megabytePerSec = bytePerSec / 1000000;
        return (long)megabytePerSec;
    }
}
