import com.minihttp.BufferPool.BufferPool;
import com.minihttp.HttpHandler.HttpHandler;
import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.HttpRequest.HttpRequest;
import com.minihttp.HttpResponse.HttpResponse;
import com.minihttp.HttpStatus.HttpStatus;
import com.minihttp.LogWrapper.LogWrapper;
import com.minihttp.Pair.Pair;
import com.minihttp.PathParameters.PathParameters;
import com.minihttp.Router.Router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Zzz {
    private final int port;
    private final AsynchronousChannelGroup channelGroup;
    private final ExecutorService executor;
    private final Router router;
    private final BufferPool bufferPool;

    public Zzz(int port) throws IOException {
        this.port = port;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.channelGroup = AsynchronousChannelGroup.withThreadPool(executor);
        router = new Router();
        bufferPool = new BufferPool();
        LogWrapper.log("[+] Server on localhost:" + port);
    }

    public void addRoute(String uri, HttpMethod method, HttpHandler handler) throws URISyntaxException {
        this.router.add(uri, method, handler);
    }

    private HttpResponse createErrorResponse(HttpStatus status) {
        return new HttpResponse.Create()
                .setStatusCode(status.getCode())
                .setEntity(Optional.of(status.getMessage()))
                .build();
    }

    private boolean isKeepAliveRequested(HttpRequest req) {
        String connectionHeaders = req.getHeader("Connection");
        return connectionHeaders != null && List.of(connectionHeaders.split(";")).stream().anyMatch(x -> x.equalsIgnoreCase("Keep-Alive"));
    }

    public void start() throws IOException {
        AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        serverChannel.bind(address);
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                serverChannel.accept(null, this);
                LogWrapper.log("[+] Accepted Connection");
                handleClient(clientChannel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                LogWrapper.log("[-] Failed to accept connection: " + exc.getMessage());
            }
        });

    }

    public Pair<HttpStatus, Pair<Boolean, HttpResponse>> requestHandler(String reqData) throws Exception {
        Pair<HttpStatus, HttpRequest> req_status_handler = HttpRequest.Create.processHttpRequest(reqData);
        if (req_status_handler.getKey() != HttpStatus.OK) {
            return new Pair<>(req_status_handler.getKey(), new Pair<>(null, null));
        }
        HttpRequest req = req_status_handler.getValue();
        Boolean isAlive = true;
        Pair<PathParameters, HttpHandler> xv = this.router.find(req.getURI(), req.getHttpMethod());
        if (xv != null) {
            HttpHandler handler = xv.getValue();
            PathParameters kv = xv.getKey();
            try {
                HttpResponse r = handler.handle(req, kv);
                return new Pair<>(req_status_handler.getKey(), new Pair<>(isAlive, r));
            } catch (Exception e) {
                return new Pair<>(HttpStatus.INTERNAL_SERVER_ERROR, new Pair<>(null, null));
            }
        } else {
            return new Pair<>(HttpStatus.NOT_FOUND, new Pair<>(null, null));
        }
    }

    public Router getRouter() {
        return this.router;
    }

    private void handleClient(AsynchronousSocketChannel clientChannel) {
        ByteBuffer buffer = bufferPool.acquireBuffer();
        clientChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead > 0) {
                    buffer.flip();
                    String message = StandardCharsets.UTF_8.decode(buffer).toString();
                    Pair<HttpStatus, Pair<Boolean, HttpResponse>> response = null;
                    try {
                        response = requestHandler(message);
                    } catch (Exception e) {
                        LogWrapper.log("[-] " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    ByteBuffer responseBuffer = null;
                    if (response.getValue().getValue() == null)
                        responseBuffer = StandardCharsets.UTF_8.encode(createErrorResponse(response.getKey()).toString());
                    else
                        responseBuffer = StandardCharsets.UTF_8.encode(response.getValue().getValue().toString());
                    Boolean keepAlive = response.getValue().getKey();

                    clientChannel.write(responseBuffer, null, new CompletionHandler<Integer, Void>() {
                        @Override
                        public void completed(Integer bytesWritten, Void attachment) {

                            if (bytesWritten > 0) {
                                try {
                                    clientChannel.close();
                                    bufferPool.release(buffer);
                                    LogWrapper.log("[-] Closing Client Connection ");
                                } catch (IOException e) {
                                    bufferPool.release(buffer);
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        @Override
                        public void failed(Throwable exc, Void attachment) {
                            LogWrapper.log("Failed to send response: " + exc.getMessage());
                            try {
                                bufferPool.release(buffer);
                                clientChannel.close();
                            } catch (IOException e) {
                                bufferPool.release(buffer);
                                LogWrapper.log("Failed to send response: " + e.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                LogWrapper.log("Failed to read from client: " + exc.getMessage());
                try {
                    bufferPool.release(buffer);
                    clientChannel.close();
                } catch (IOException e) {
                    bufferPool.release(buffer);
                    LogWrapper.log("Failed to read from client: " + e.getMessage());
                }
            }
        });
    }
}
