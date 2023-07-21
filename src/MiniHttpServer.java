import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.HttpRequest.HttpRequest;
import com.minihttp.HttpResponse.HttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class MiniHttpServer {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final ByteBuffer buffer;
    private final ExecutorService executor;

    private final Map<URI, AbstractMap.SimpleImmutableEntry<HttpMethod, Function<HttpRequest, HttpResponse>>> routes;

    MiniHttpServer(Integer port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        buffer = ByteBuffer.allocate(1024);
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());// Adjust the thread pool size as needed
        routes = new HashMap<>();
    }

    public void addRoute(URI k, AbstractMap.SimpleImmutableEntry<HttpMethod, Function<HttpRequest, HttpResponse>> r) {
        this.routes.put(k, r);
    }

    public void requestHandler(String reqData, SocketChannel client) throws Exception {
        HttpRequest req = HttpRequest.Create.fromStringRequest(reqData);
        AbstractMap.SimpleImmutableEntry<HttpMethod, Function<HttpRequest, HttpResponse>> _route = this.routes.get(req.getURI());
        if (_route != null) {
            Function<HttpRequest, HttpResponse> h = _route.getValue();
            HttpMethod reqType = _route.getKey();
            if (reqType != req.getHttpMethod()) {
                try {
                    client.write(ByteBuffer.wrap(
                            new HttpResponse.Create()
                                    .setStatusCode(405)
                                    .setEntity(Optional.of("Method Not Allowed"))
                                    .build().toString().getBytes()));
                    client.close();
                } catch (ClosedChannelException e) {
                    client.close();
                    throw new RuntimeException(e);
                }
                return;
            }
            CompletableFuture<HttpResponse> resp_f = CompletableFuture.supplyAsync(() -> h.apply(req), executor);
            resp_f.thenAccept(r -> {
                boolean keepAlive = false;
                List<String> connectionHeaders = req.getHeader("Connection");
                if (connectionHeaders != null && connectionHeaders.contains("Keep-Alive")) {
                    keepAlive = true;
                }

                HttpResponse.Create responseBuilder = new HttpResponse.Create()
                        .setStatusCode(200)
                        .setResponseHeader(req.getRequestHeader())
                        .setEntity(Optional.of("Hello"));

                String response = responseBuilder.build().toString();
                try {
                    client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));

                    if (!keepAlive) {
                        client.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (!keepAlive) {
                        try {
                            client.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } else {
            try {
                client.write(ByteBuffer.wrap(new HttpResponse.Create()
                        .setStatusCode(404).setEntity(Optional.of("Route not found"))
                        .build().toString().getBytes()));
                client.close();
            } catch (ClosedChannelException e) {
                client.close();
                throw new RuntimeException(e);
            }
        }

    }

    void handleReadKey(SelectionKey key) throws Exception {
        SocketChannel client = (SocketChannel) key.channel(); // get client channel
        StringBuilder req = (StringBuilder) key.attachment(); // get the attached object to channel

        int bytesRead;
        try {
            bytesRead = client.read(buffer);
        } catch (IOException e) {
            bytesRead = -1;
        }
        if (bytesRead == -1) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (bytesRead > 0) {
            buffer.flip();

            byte[] rData = new byte[buffer.remaining()];
            buffer.get(rData);
            req.append(new String(rData, StandardCharsets.UTF_8));

            int endOfRequest = req.indexOf("\r\n\r\n"); // processign partal request like if ...\r\n\r\n .... \r\n\r\n , two requests in single read
            if (endOfRequest >= 0) {
                String remainingRequest = req.substring(endOfRequest + 4);
                key.attach(new StringBuilder(remainingRequest)); // attach data for second req if any in same read operation

                String requestData = req.substring(0, endOfRequest + 4);
                this.requestHandler(requestData, client);

            } else {
                try {
                    client.register(selector, SelectionKey.OP_READ, req);
                } catch (Exception e) {
                    client.close();
                    e.printStackTrace();
                }
            }
            buffer.clear();
        }
    }

    void handleWriteKey(SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        ByteBuffer responseBuffer = (ByteBuffer) key.attachment();
        try {
            clientSocket.write(responseBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (responseBuffer.remaining() > 0) {
            // Not all data has been written, register for write interest again
            try {
                clientSocket.register(selector, SelectionKey.OP_WRITE, responseBuffer);
            } catch (Exception e) {
                clientSocket.close();
            }
        } else {
            // All data has been written, close the connection
            try {
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void handleAcceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = serverSocket.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new StringBuilder());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    } catch (Exception e) {
                        // Handle URISyntaxException
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            // Handle IOException while selecting keys
            e.printStackTrace();
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