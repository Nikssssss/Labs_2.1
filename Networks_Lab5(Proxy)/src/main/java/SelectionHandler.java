import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SelectionHandler {
    private int BUFFER_SIZE = 10000;

    public void handle(SelectionKey selectionKey){
        try {
            if (selectionKey.isAcceptable()) {
                acceptClient(selectionKey);
            } else if (selectionKey.isReadable()){
                read(selectionKey);
            } else if (selectionKey.isConnectable()){
                finishConnection(selectionKey);
            } else if (selectionKey.isWritable()){
                write(selectionKey);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void acceptClient(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
        System.out.println("Accepted");
    }

    private void read(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((SocketChannel)selectionKey.channel());
        KeyAttachment keyAttachment = (KeyAttachment) selectionKey.attachment();
        if (keyAttachment == null){
            keyAttachment = new KeyAttachment(ByteBuffer.allocate(BUFFER_SIZE), ByteBuffer.allocate(BUFFER_SIZE));
            selectionKey.attach(keyAttachment);
        }
        int count = socketChannel.read(keyAttachment.getInput());
        if (count > 0){
            if (keyAttachment.getRemoteServer() == null) {
                switch (keyAttachment.getClientMessageStatus()) {
                    case GREETING -> {
                        if (count > 2) {
                            byte[] message = keyAttachment.getInput().array();
                            int nauth = message[1];
                            if (count == nauth + 2) {
                                keyAttachment.getOutput().put(new byte[]{0x05, 0x00}).flip(); //no authentication
                                keyAttachment.getInput().clear();
                                selectionKey.interestOps(SelectionKey.OP_WRITE);
                                keyAttachment.setClientMessageStatus(ClientMessageStatus.CONNECTION_REQUEST);
                                System.out.println("Read greeting");
                            }
                        }
                    }
                    case CONNECTION_REQUEST -> {
                        if (count > 5) {
                            byte[] message = keyAttachment.getInput().array();
                            if (message[1] == 0x01) { // tcp/ip stream connection
                                int type = message[3];
                                if (type == 0x01) { //IPv4 address
                                    if (count == 10) {
                                        int[] intIP = new int[]{Byte.toUnsignedInt(message[4]), Byte.toUnsignedInt(message[5]),
                                                Byte.toUnsignedInt(message[6]), Byte.toUnsignedInt(message[7])};
                                        String stringIP = String.valueOf(intIP[0]) + "." + String.valueOf(intIP[1])
                                                + "." + String.valueOf(intIP[2]) + "." + String.valueOf(intIP[3]);
                                        byte[] port = new byte[]{0, 0, message[8], message[9]};
                                        SocketChannel remoteServer = SocketChannel.open();
                                        remoteServer.configureBlocking(false);
                                        System.out.println(stringIP);
                                        remoteServer.connect(new InetSocketAddress(stringIP, ByteBuffer.wrap(port).getInt()));
                                        SelectionKey remoteServerKey = remoteServer.register(selectionKey.selector(), SelectionKey.OP_CONNECT);
                                        selectionKey.interestOps(0);
                                        ((KeyAttachment) selectionKey.attachment()).setRemoteServer(remoteServerKey);
                                        KeyAttachment remoteServerAttachment = new KeyAttachment(ByteBuffer.allocate(BUFFER_SIZE), null);
                                        remoteServerAttachment.setRemoteServer(selectionKey);
                                        remoteServerKey.attach(remoteServerAttachment);
                                        ((KeyAttachment) selectionKey.attachment()).getInput().clear();
                                        keyAttachment.setClientMessageStatus(ClientMessageStatus.REMOTE);
                                        System.out.println("Read connection");
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                keyAttachment.getRemoteServer().interestOps(SelectionKey.OP_WRITE | keyAttachment.getRemoteServer().interestOps());
                selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_READ);
                keyAttachment.getInput().flip();
                System.out.println("Read");
            }
        }
        else {
            stop(selectionKey);
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((SocketChannel)selectionKey.channel());
        KeyAttachment keyAttachment = (KeyAttachment)selectionKey.attachment();
        int count = socketChannel.write(keyAttachment.getOutput());
        if (count < 1) {
            stop(selectionKey);
        }
        if (!keyAttachment.getOutput().hasRemaining()){
            if (keyAttachment.getRemoteServer() == null){
                if (keyAttachment.getClientMessageStatus() == ClientMessageStatus.REMOTE){
                    stop(selectionKey);
                } else {
                    selectionKey.interestOps(SelectionKey.OP_READ);
                    keyAttachment.getOutput().clear();
                }
            }
            else if (keyAttachment.getClientMessageStatus() == ClientMessageStatus.CONNECTION_FAILURE) {
                stopPair(selectionKey);
            }
            else {
                keyAttachment.getRemoteServer().interestOps(SelectionKey.OP_READ | keyAttachment.getRemoteServer().interestOps());
                selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_WRITE);
                keyAttachment.getOutput().clear();
            }
        }
    }

    private void finishConnection(SelectionKey selectionKey) {
        SocketChannel socketChannel = ((SocketChannel)selectionKey.channel());
        KeyAttachment remoteAttachment = (KeyAttachment)selectionKey.attachment();
        try {
            socketChannel.finishConnect();
            remoteAttachment.setOutput(((KeyAttachment) remoteAttachment.getRemoteServer().attachment()).getInput());
            ((KeyAttachment) remoteAttachment.getRemoteServer().attachment()).setOutput(remoteAttachment.getInput());
            InetSocketAddress socketAddress = (InetSocketAddress) ((SocketChannel)remoteAttachment.getRemoteServer().channel()).getLocalAddress();
            byte[] ip = socketAddress.getAddress().getAddress();
            byte[] port = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(socketAddress.getPort()).array(), 2, 4);
            remoteAttachment.getInput().put(createSuccessResponse(ip, port));
            remoteAttachment.getInput().flip();
            selectionKey.interestOps(0);
            remoteAttachment.getRemoteServer().interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException e) {
            InetSocketAddress socketAddress;
            try {
                socketAddress = (InetSocketAddress) ((SocketChannel)remoteAttachment.getRemoteServer().channel()).getLocalAddress();
            } catch (IOException ignored){
                return;
            }
            remoteAttachment.setOutput(((KeyAttachment) remoteAttachment.getRemoteServer().attachment()).getInput());
            ((KeyAttachment) remoteAttachment.getRemoteServer().attachment()).setOutput(remoteAttachment.getInput());
            byte[] ip = socketAddress.getAddress().getAddress();
            byte[] port = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(socketAddress.getPort()).array(), 2, 4);
            remoteAttachment.getInput().put(createFailedResponse(ip, port));
            remoteAttachment.getInput().flip();
            selectionKey.interestOps(0);
            remoteAttachment.getRemoteServer().interestOps(SelectionKey.OP_WRITE);
            ((KeyAttachment) remoteAttachment.getRemoteServer().attachment()).setClientMessageStatus(ClientMessageStatus.CONNECTION_FAILURE);
        }

    }

    private byte[] createSuccessResponse(byte[] ip, byte[] port){
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{0x05, 0x00, 0x00, 0x01});
        byteBuffer.put(ip);
        byteBuffer.put(port);
        return byteBuffer.array();
    }

    private byte[] createFailedResponse(byte[] ip, byte[] port){
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{0x05, 0x01, 0x00, 0x01});
        byteBuffer.put(ip);
        byteBuffer.put(port);
        return byteBuffer.array();
    }

    private void stop(SelectionKey selectionKey){
        selectionKey.cancel();
        try {
            selectionKey.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SelectionKey remoteServer = ((KeyAttachment) selectionKey.attachment()).getRemoteServer();
        if (remoteServer != null) {
            ((KeyAttachment)remoteServer.attachment()).setRemoteServer(null);
            if ((remoteServer.interestOps() & SelectionKey.OP_WRITE) == 0) {
                ((KeyAttachment)remoteServer.attachment()).getOutput().flip();
            }
            remoteServer.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void stopPair(SelectionKey selectionKey){
        selectionKey.cancel();
        try {
            selectionKey.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SelectionKey remoteServer = ((KeyAttachment) selectionKey.attachment()).getRemoteServer();
        if (remoteServer != null){
            remoteServer.cancel();
            try {
                remoteServer.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
