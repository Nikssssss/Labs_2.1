import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class SelectionReceiver {
    private SelectionHandler selectionHandler;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public SelectionReceiver(int port) throws IOException {
        selectionHandler = new SelectionHandler();
        selector = SelectorProvider.provider().openSelector();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1", port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() {
        while (!Thread.currentThread().isInterrupted()){
            try {
                selector.select();
                var selectionIterator = selector.selectedKeys().iterator();
                while (selectionIterator.hasNext()){
                    SelectionKey selectionKey = selectionIterator.next();
                    selectionIterator.remove();
                    selectionHandler.handle(selectionKey);
                }
            } catch (IOException e) {
                break;
            }
        }
    }
}
