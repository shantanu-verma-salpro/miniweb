
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Iterator;
import java.net.URI;
enum HttpMethod {
    GET,
    PUT,
    POST,
    PATCH
}
class HttpResponse{
    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getResponseHeader() {
        return responseHeader;
    }

    public Optional<Object> getEntity() {
        return entity;
    }

    private final Integer statusCode;
    private final Map<String,List<String>> responseHeader;
    private final Optional<Object> entity;
    private HttpResponse(Integer s,Map<String,List<String>> rh,Optional<Object> e){
        this.statusCode = s;
        this.responseHeader = rh;
        this.entity = e;
    }
    public static class Create{
        private  Integer statusCode;
        private Map<String,List<String>> responseHeader;
        private Optional<Object> entity;
        public Create(){
            this.responseHeader = new HashMap<>();
            this.statusCode = 200;
            this.entity = Optional.of("");
        }
        public void setStatusCode(Integer s){
            this.statusCode = s;
        }
        public void setResponseHeader(Map<String,List<String>> ls){
            this.responseHeader = ls;
        }
        public void setEntity(Optional<Object> e){
            this.entity = e;
        }
        public HttpResponse build(){
            return new HttpResponse(this.statusCode,this.responseHeader,this.entity);
        }

    }
}
class HttpRequest{
    private final URI uri;
    private final Map<String,List<String>> requestHeader;
    private final HttpMethod opCode;
    private HttpRequest(HttpMethod op,Map<String,List<String>> reqHeader,URI u){
        this.uri = u;
        this.opCode = op;
        this.requestHeader = reqHeader;
    }
    public URI getURI(){
        return this.uri;
    }
    public HttpMethod getHttpMethod(){
        return this.opCode;
    }
    public Map<String,List<String>> getRequestHeader(){
        return requestHeader;
    }
    public static class Create{
        private URI uri;
        private Map<String,List<String>> requestHeader;
        private HttpMethod opCode;

        public Create() {
        }
        public void setURI(URI u){
            this.uri = u;
        }
        public void setHttpMethod(HttpMethod m){
            this.opCode = m;
        }
        public void setRequestHeader(Map<String,List<String>> l){
            this.requestHeader = l;
        }
        public HttpRequest build(){
            return new HttpRequest(this.opCode,this.requestHeader,this.uri);
        }

    };
};


class NioHttpServer {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final ByteBuffer buffer;
    private HttpRequest req;
    private HttpResponse resp;

    private void parseIntoRequest(String r){
        String[] mes = r.split("\\r?\\n");
        System.out.println(mes[0]);
    }

    NioHttpServer(Integer port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        buffer = ByteBuffer.allocate(1024);
    }

    void handleReadKey(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        StringBuilder req = (StringBuilder) key.attachment();

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            // Handle end-of-stream condition
            client.close();
        } else if (bytesRead > 0) {
            buffer.flip();

            byte[] rData = new byte[buffer.remaining()];
            buffer.get(rData);
            req.append(new String(rData));
            if (req.toString().contains("\r\n\r\n")) {
                this.parseIntoRequest(req.toString());
                String resp = "HTTP/1.1 200 OK\r\nContent-Length:12\r\n\r\nHello World!";
                client.register(selector, SelectionKey.OP_WRITE, ByteBuffer.wrap(resp.getBytes()));
            } else {
                client.register(selector, SelectionKey.OP_READ, req);
            }
            buffer.clear();
        }
    }

    void handleWriteKey(SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        ByteBuffer responseBuffer = (ByteBuffer) key.attachment();
        clientSocket.write(responseBuffer);
        if (responseBuffer.remaining() > 0) {
            // Not all data has been written, register for write interest again
            clientSocket.register(selector, SelectionKey.OP_WRITE, responseBuffer);
        } else {
            // All data has been written, close the connection
            clientSocket.close();
        }
    }

    void handleAcceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, new StringBuilder());
    }

    void start() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                if (key.isAcceptable()) {
                    handleAcceptKey(key);
                }
                if (key.isReadable()) {
                    handleReadKey(key);
                } else if (key.isWritable()) {
                    handleWriteKey(key);
                }
                selectedKeys.remove();
            }
        }
    }
}

public class Main {

    public static void main(String[] args) throws IOException {
        NioHttpServer server = new NioHttpServer(5000);
        server.start();
    }
}
