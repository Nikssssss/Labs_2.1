import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class DnsAttachment {
    private final int BUFFER_SIZE = 2048;
    private final SelectionKey dnsChannelKey;
    private final ByteBuffer input;
    private final HashMap<Integer, DnsResolve> sentDnsResolves;
    private final ArrayDeque<DnsResolve> resolvesToSend;
    private int requestID = 0;

    public DnsAttachment(SelectionKey dnsChannelKey) {
        this.dnsChannelKey = dnsChannelKey;
        input = ByteBuffer.allocate(BUFFER_SIZE);
        sentDnsResolves = new HashMap<>();
        resolvesToSend = new ArrayDeque<>();
        dnsChannelKey.interestOps(SelectionKey.OP_READ);
    }

    public void addResolve(String domain, BasicAttachment basicAttachment){
        resolvesToSend.add(new DnsResolve(domain, basicAttachment));
        dnsChannelKey.interestOps(dnsChannelKey.interestOps() | SelectionKey.OP_WRITE);
    }

    private int getIndex(){
        return requestID++;
    }

    public Map.Entry<Integer, DnsResolve> popResolve(){
        if (resolvesToSend.isEmpty()){
            dnsChannelKey.interestOps(dnsChannelKey.interestOps() ^ SelectionKey.OP_WRITE);
            return null;
        } else {
            DnsResolve dnsResolve = resolvesToSend.pop();
            sentDnsResolves.put(requestID, dnsResolve);
            return new Map.Entry<>() {
                @Override
                public Integer getKey() {
                    return getIndex();
                }

                @Override
                public DnsResolve getValue() {
                    return dnsResolve;
                }

                @Override
                public DnsResolve setValue(DnsResolve value) {
                    return null;
                }
            };
        }
    }

    public ByteBuffer getInput() {
        return input;
    }

    public DnsResolve getSentResolve(int requestId){
        return sentDnsResolves.get(requestId);
    }

    public void removeSentResolve(int requestId){
        sentDnsResolves.remove(requestId);
    }
}
