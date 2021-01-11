import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class SelectionReceiver {
    private final SelectionHandler selectionHandler;
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final DatagramChannel dnsChannel;

    public SelectionReceiver(int port) throws IOException {
        selector = SelectorProvider.provider().openSelector();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1", port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        dnsChannel = DatagramChannel.open();
        dnsChannel.configureBlocking(false);
        SelectionKey dnsChannelKey = dnsChannel.register(selector, SelectionKey.OP_READ);
        DnsAttachment dnsAttachment = new DnsAttachment(dnsChannelKey);
        dnsChannelKey.attach(dnsAttachment);
        selectionHandler = new SelectionHandler(dnsAttachment);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocketChannel.close();
                selector.close();
            } catch (IOException ignored){}
        }));
    }

    public void start() {
        try {
            while (selector.select() > -1) {
                var selectionIterator = selector.selectedKeys().iterator();
                while (selectionIterator.hasNext()) {
                    SelectionKey selectionKey = selectionIterator.next();
                    selectionIterator.remove();
                    if (!selectionKey.isValid()){
                        continue;
                    }
                    selectionHandler.handle(selectionKey);
                }
            }
        } catch (IOException ignored) {}
    }
}

