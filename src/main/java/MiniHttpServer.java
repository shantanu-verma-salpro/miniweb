import com.minihttp.HttpHandler.HttpHandler;
import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.HttpRequest.HttpRequest;
import com.minihttp.HttpResponse.HttpResponse;
import com.minihttp.HttpStatus.HttpStatus;
import com.minihttp.Pair.Pair;
import com.minihttp.PathParameters.PathParameters;
import com.minihttp.Router.Router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MiniHttpServer {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final ByteBuffer buffer;
    private final ExecutorService executor;

    private final Router router;

    MiniHttpServer(Integer port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        buffer = ByteBuffer.allocate(2048);
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());// Adjust the thread pool size as needed
        router = new Router();
    }

    public void addRoute(String uri, HttpMethod method, HttpHandler handler) throws URISyntaxException {
        this.router.add(uri, method, handler);
    }

    public void sendErrorResponse(SocketChannel client, HttpStatus status) throws IOException {
        sendErrorResponse(client, status.getCode(), status.getMessage());
    }

    public void requestHandler(String reqData, SocketChannel client) throws Exception {
        Pair<HttpStatus, HttpRequest> req_status_handler = HttpRequest.Create.processHttpRequest(reqData);
        if (req_status_handler.getKey() != HttpStatus.OK) {
            sendErrorResponse(client, req_status_handler.getKey());
            return;
        }

        HttpRequest req = req_status_handler.getValue();
        Pair<PathParameters, HttpHandler> xv = this.router.find(req.getURI(), req.getHttpMethod());
        if (xv != null) {
            HttpHandler handler = xv.getValue();
            PathParameters kv = xv.getKey();
            try {
                HttpResponse r = handler.handle(req, kv);
                boolean keepAlive = isKeepAliveRequested(req);
                try {
                    client.write(ByteBuffer.wrap(r.toString().getBytes(StandardCharsets.UTF_8)));
                    if (!keepAlive) {
                        client.close();
                        System.out.println("[+] Connection to client closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                sendErrorResponse(client, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            sendErrorResponse(client, HttpStatus.NOT_FOUND);
        }
    }

    private HttpResponse createErrorResponse(HttpStatus status, String message) {
        return new HttpResponse.Create()
                .setStatusCode(status.getCode())
                .setEntity(Optional.of(message))
                .build();
    }

    private boolean isKeepAliveRequested(HttpRequest req) {
        String connectionHeaders = req.getHeader("Connection");
        return connectionHeaders != null && List.of(connectionHeaders.split(";")).stream().anyMatch(x -> x.equalsIgnoreCase("Keep-Alive"));
    }

    private void sendErrorResponse(SocketChannel client, int statusCode, String message) throws IOException {
        client.write(ByteBuffer.wrap(createErrorResponse(Objects.requireNonNull(HttpStatus.fromCode(statusCode)), message).toString().getBytes(StandardCharsets.UTF_8)));
        client.close();
    }


    public void handleReadKey(SelectionKey key) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        SocketChannel clientChannel = (SocketChannel) key.channel();

        try {
            // Read data from the client channel
            int bytesRead = clientChannel.read(buffer);

            // Check for closed connection
            if (bytesRead == -1) {
                // Connection closed by the client, handle this scenario
                clientChannel.close();
                return;
            }

            while (bytesRead > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                requestBuilder.append(new String(bytes, StandardCharsets.UTF_8));
                buffer.clear();
                bytesRead = clientChannel.read(buffer);
            }

            this.requestHandler(requestBuilder.toString(), clientChannel);
        } catch (Exception e) {
            // Handle IO error, such as network issues
            e.printStackTrace();
            clientChannel.close();
            return;
        }

        // If everything is successful, clear the buffer and register the channel back for reading.
        buffer.clear();
        try {
            clientChannel.register(key.selector(), SelectionKey.OP_READ);
        } catch (Exception e) {
            clientChannel.close();
        }
    }


    void handleWriteKey(SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        ByteBuffer responseBuffer = (ByteBuffer) key.attachment();
        try {
            clientSocket.write(responseBuffer);
            if (responseBuffer.remaining() == 0) {
                // All data has been written, close the connection
                clientSocket.close();
            } else {
                // Not all data has been written, register for write interest again
                clientSocket.register(selector, SelectionKey.OP_WRITE, responseBuffer);
            }
        } catch (IOException e) {
            // Handle error while writing
            clientSocket.close();
        }
    }

    void handleAcceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, new StringBuilder());
    }


    void start() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove(); // Move the remove operation here to avoid ConcurrentModificationException

                    try {
                        if (key.isAcceptable()) {
                            handleAcceptKey(key);
                        }
                        if (key.isReadable()) {
                            handleReadKey(key);
                        } else if (key.isWritable()) {
                            handleWriteKey(key);
                        }
                    } catch (IOException e) {
                        // Handle IO exception while handling a key
                        closeClientSocket((SocketChannel) key.channel());
                        System.err.println("[+] Connection to client closed 209");
                    } catch (Exception e) {
                        // Handle URISyntaxException
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            // Handle IOException while selecting keys
            e.printStackTrace();
        } finally {
            this.stop();
        }
    }

    public void stop() {
        try {
            for (SelectionKey key : selector.keys()) {
                closeClientSocket((SocketChannel) key.channel());
            }
            selector.close();
            serverSocketChannel.close();
            executor.shutdown();
            System.err.println("[+] Closing Server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeClientSocket(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            // Handle if any error occurs while closing the channel
        }
    }
}