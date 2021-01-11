import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SelectionHandler {
    private final int BUFFER_SIZE = 2048;
    private final int GREETING_CONSTANT_PART = 2;
    private final int CONNECTION_CONSTANT_PART = 5;
    private final int IPV4_SIZE = 10;
    private final int PORT_SIZE = 2;
    private final DnsAttachment dnsAttachment;
    private final InetSocketAddress dnsAddress;

    public SelectionHandler(DnsAttachment dnsAttachment) {
        this.dnsAttachment = dnsAttachment;
        dnsAddress = ResolverConfig.getCurrentConfig().servers().get(0);
    }

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
            stop(selectionKey);
        }
    }

    private void acceptClient(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
    }

    private void read(SelectionKey selectionKey) throws IOException {
        if (selectionKey.attachment() instanceof DnsAttachment){
            readFromDns(selectionKey);
        } else {
            SocketChannel socketChannel = ((SocketChannel) selectionKey.channel());
            BasicAttachment basicAttachment = (BasicAttachment) selectionKey.attachment();
            if (basicAttachment == null) {
                basicAttachment = new BasicAttachment(ByteBuffer.allocate(BUFFER_SIZE), ByteBuffer.allocate(BUFFER_SIZE), selectionKey);
                selectionKey.attach(basicAttachment);
            }
            int count = socketChannel.read(basicAttachment.getInput());
            if (count > 0) {
                if (basicAttachment.getRemoteServer() == null) {
                    switch (basicAttachment.getClientMessageStatus()) {
                        case GREETING -> {
                            handleGreeting(count, basicAttachment, selectionKey);
                        }
                        case CONNECTION_REQUEST -> {
                            if (count > CONNECTION_CONSTANT_PART) {
                                byte[] message = basicAttachment.getInput().array();
                                if (message[1] == 0x01) { // tcp/ip stream connection
                                    int type = message[3];
                                    if (type == 0x01) { //IPv4 address
                                        handleIPv4Connection(count, message, selectionKey, basicAttachment);
                                    } else if (type == 0x03) { //domain name
                                        handleDomainConnection(count, message, selectionKey, basicAttachment);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    handleData(selectionKey, basicAttachment);
                }
            } else {
                stop(selectionKey);
            }
        }
    }

    private void handleGreeting(int count, BasicAttachment basicAttachment, SelectionKey selectionKey){
        if (count > GREETING_CONSTANT_PART) {
            byte[] message = basicAttachment.getInput().array();
            int nauth = message[1];
            if (count == nauth + GREETING_CONSTANT_PART) {
                basicAttachment.getOutput().put(new byte[]{0x05, 0x00}).flip(); //no authentication
                basicAttachment.getInput().clear();
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                basicAttachment.setClientMessageStatus(ClientMessageStatus.CONNECTION_REQUEST);
            }
        }
    }

    private void handleIPv4Connection(int count, byte[] message, SelectionKey selectionKey, BasicAttachment basicAttachment) throws IOException {
        if (count == IPV4_SIZE) {
            int[] intIP = new int[]{Byte.toUnsignedInt(message[4]), Byte.toUnsignedInt(message[5]),
                    Byte.toUnsignedInt(message[6]), Byte.toUnsignedInt(message[7])};
            String stringIP = intIP[0] + "." + intIP[1] + "." + intIP[2] + "." + intIP[3];
            byte[] port = new byte[]{0, 0, message[8], message[9]};
            SocketChannel remoteServer = SocketChannel.open();
            remoteServer.configureBlocking(false);
            remoteServer.connect(new InetSocketAddress(stringIP, ByteBuffer.wrap(port).getInt()));
            SelectionKey remoteServerKey = remoteServer.register(selectionKey.selector(), SelectionKey.OP_CONNECT);
            selectionKey.interestOps(0);
            ((BasicAttachment) selectionKey.attachment()).setRemoteServer(remoteServerKey);
            BasicAttachment remoteServerAttachment = new BasicAttachment(ByteBuffer.allocate(BUFFER_SIZE), null, null);
            remoteServerAttachment.setRemoteServer(selectionKey);
            remoteServerKey.attach(remoteServerAttachment);
            ((BasicAttachment) selectionKey.attachment()).getInput().clear();
            basicAttachment.setClientMessageStatus(ClientMessageStatus.REMOTE);
        }
    }

    private void handleDomainConnection(int count, byte[] message, SelectionKey selectionKey, BasicAttachment basicAttachment){
        int domainLength = message[4];
        if (count == domainLength + CONNECTION_CONSTANT_PART + PORT_SIZE) {
            String domain = new String(Arrays.copyOfRange(message, CONNECTION_CONSTANT_PART, CONNECTION_CONSTANT_PART + domainLength), StandardCharsets.UTF_8);
            int port = ByteBuffer.wrap(new byte[]{0, 0, message[count - 2], message[count - 1]}).getInt();
            basicAttachment.setServerPort(port);
            basicAttachment.setClientMessageStatus(ClientMessageStatus.REMOTE);
            dnsAttachment.addResolve(domain, basicAttachment);
            selectionKey.interestOps(0);
            basicAttachment.getInput().clear();
        }
    }

    private void handleData(SelectionKey selectionKey, BasicAttachment basicAttachment){
        basicAttachment.getRemoteServer().interestOps(SelectionKey.OP_WRITE | basicAttachment.getRemoteServer().interestOps());
        selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_READ);
        basicAttachment.getInput().flip();
    }

    private void readFromDns(SelectionKey selectionKey) throws IOException {
        DatagramChannel datagramChannel = ((DatagramChannel)selectionKey.channel());
        dnsAttachment.getInput().clear();
        datagramChannel.receive(dnsAttachment.getInput());
        dnsAttachment.getInput().flip();
        Message message = new Message(dnsAttachment.getInput().array());
        if (message.getRcode() == Rcode.NOERROR) {
            int requestID = message.getHeader().getID();
            DnsResolve dnsResolve = dnsAttachment.getSentResolve(requestID);
            if (dnsResolve != null) {
                List<Record> answers = message.getSection(Section.ANSWER);
                ARecord aRecord = null;
                for (Record record : answers) {
                    if (record.getType() == Type.A) {
                        aRecord = (ARecord) record;
                        break;
                    }
                }
                BasicAttachment basicAttachment = dnsResolve.getBasicAttachment();
                if (aRecord != null) {
                    InetAddress serverAddress = aRecord.getAddress();
                    SocketChannel remoteServer = SocketChannel.open();
                    remoteServer.configureBlocking(false);
                    remoteServer.connect(new InetSocketAddress(serverAddress, basicAttachment.getServerPort()));
                    SelectionKey remoteServerKey = remoteServer.register(selectionKey.selector(), SelectionKey.OP_CONNECT);
                    ((BasicAttachment) basicAttachment.getOwnKey().attachment()).setRemoteServer(remoteServerKey);
                    BasicAttachment remoteServerAttachment = new BasicAttachment(ByteBuffer.allocate(BUFFER_SIZE), null, null);
                    remoteServerAttachment.setRemoteServer(basicAttachment.getOwnKey());
                    remoteServerKey.attach(remoteServerAttachment);
                } else {
                    InetSocketAddress socketAddress = (InetSocketAddress) ((SocketChannel) (basicAttachment.getOwnKey().channel())).getLocalAddress();
                    byte[] ip = socketAddress.getAddress().getAddress();
                    byte[] port = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(socketAddress.getPort()).array(), 2, 4);
                    byte[] response = createFailedResponse(ip, port, (byte) 0x04);
                    ((BasicAttachment) (basicAttachment.getOwnKey().attachment())).getOutput().put(response);
                    basicAttachment.getOwnKey().interestOps(basicAttachment.getOwnKey().interestOps() | SelectionKey.OP_WRITE);
                }
                dnsAttachment.removeSentResolve(requestID);
            }
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
        if (selectionKey.attachment() instanceof DnsAttachment){
            writeToDnsServer(selectionKey);
        } else {
            SocketChannel socketChannel = ((SocketChannel)selectionKey.channel());
            BasicAttachment basicAttachment = (BasicAttachment)selectionKey.attachment();
            int count = socketChannel.write(basicAttachment.getOutput());
            if (count < 1) {
                stop(selectionKey);
                return;
            }
            if (!basicAttachment.getOutput().hasRemaining()) {
                if (basicAttachment.getRemoteServer() == null) {
                    if (basicAttachment.getClientMessageStatus() == ClientMessageStatus.REMOTE) {
                        stop(selectionKey);
                    } else {
                        selectionKey.interestOps(SelectionKey.OP_READ);
                        basicAttachment.getOutput().clear();
                    }
                } else if (basicAttachment.getClientMessageStatus() == ClientMessageStatus.CONNECTION_FAILURE) {
                    stopPair(selectionKey);
                } else {
                    basicAttachment.getRemoteServer().interestOps(SelectionKey.OP_READ | basicAttachment.getRemoteServer().interestOps());
                    selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_WRITE);
                    basicAttachment.getOutput().clear();
                }
            }
        }
    }

    private void writeToDnsServer(SelectionKey selectionKey) throws IOException {
        Map.Entry<Integer, DnsResolve> dnsResolveEntry = dnsAttachment.popResolve();
        if (dnsResolveEntry != null){
            Message message = new Message();
            Header header = message.getHeader();
            header.setOpcode(Opcode.QUERY);
            header.setFlag(Flags.RD);
            header.setID(dnsResolveEntry.getKey());
            message.addRecord(Record.newRecord(new Name(dnsResolveEntry.getValue().getDomain() + "."), Type.A, DClass.IN), Section.QUESTION);
            ByteBuffer buffer = ByteBuffer.wrap(message.toWire());
            ((DatagramChannel) (selectionKey.channel())).send(buffer, dnsAddress);
        }
    }

    private void finishConnection(SelectionKey selectionKey) {
        SocketChannel socketChannel = ((SocketChannel)selectionKey.channel());
        BasicAttachment remoteAttachment = (BasicAttachment)selectionKey.attachment();
        try {
            socketChannel.finishConnect();
            remoteAttachment.setOutput(((BasicAttachment) remoteAttachment.getRemoteServer().attachment()).getInput());
            ((BasicAttachment) remoteAttachment.getRemoteServer().attachment()).setOutput(remoteAttachment.getInput());
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
            remoteAttachment.setOutput(((BasicAttachment) remoteAttachment.getRemoteServer().attachment()).getInput());
            ((BasicAttachment) remoteAttachment.getRemoteServer().attachment()).setOutput(remoteAttachment.getInput());
            byte[] ip = socketAddress.getAddress().getAddress();
            byte[] port = Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(socketAddress.getPort()).array(), 2, 4);
            remoteAttachment.getInput().put(createFailedResponse(ip, port));
            remoteAttachment.getInput().flip();
            selectionKey.interestOps(0);
            remoteAttachment.getRemoteServer().interestOps(SelectionKey.OP_WRITE);
            ((BasicAttachment) remoteAttachment.getRemoteServer().attachment()).setClientMessageStatus(ClientMessageStatus.CONNECTION_FAILURE);
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

    private byte[] createFailedResponse(byte[] ip, byte[] port, byte type){
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{0x05, type, 0x00, 0x01});
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
        SelectionKey remoteServer = ((BasicAttachment) selectionKey.attachment()).getRemoteServer();
        if (remoteServer != null) {
            ((BasicAttachment)remoteServer.attachment()).setRemoteServer(null);
            if ((remoteServer.interestOps() & SelectionKey.OP_WRITE) == 0) {
                ((BasicAttachment)remoteServer.attachment()).getOutput().flip();
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
        SelectionKey remoteServer = ((BasicAttachment) selectionKey.attachment()).getRemoteServer();
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
