import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class BasicAttachment {
    private ByteBuffer input;
    private ByteBuffer output;
    private ClientMessageStatus clientMessageStatus;
    private SelectionKey remoteServer;
    private SelectionKey ownKey;
    private int serverPort;

    public BasicAttachment(ByteBuffer input, ByteBuffer output, SelectionKey ownKey){
        clientMessageStatus = ClientMessageStatus.GREETING;
        this.input = input;
        this.output = output;
        this.ownKey = ownKey;
    }

    public ByteBuffer getInput() {
        return input;
    }

    public ByteBuffer getOutput() {
        return output;
    }

    public ClientMessageStatus getClientMessageStatus() {
        return clientMessageStatus;
    }

    public void setOutput(ByteBuffer output) {
        this.output = output;
    }

    public void setClientMessageStatus(ClientMessageStatus clientMessageStatus) {
        this.clientMessageStatus = clientMessageStatus;
    }

    public SelectionKey getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(SelectionKey remoteServer) {
        this.remoteServer = remoteServer;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getServerPort(){
        return serverPort;
    }

    public SelectionKey getOwnKey(){
        return ownKey;
    }
}
