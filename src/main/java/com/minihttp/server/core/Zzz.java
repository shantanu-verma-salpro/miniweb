package com.minihttp.server.core;

import com.minihttp.LogWrapper.LogWrapper;
import com.minihttp.PathParameters.PathParameters;
import com.minihttp.handlers.HttpHandler.HttpHandler;
import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.http.HttpRequest.HttpRequest;
import com.minihttp.http.HttpResponse.HttpResponse;
import com.minihttp.http.HttpStatus.HttpStatus;
import com.minihttp.routing.Router.Router;
import com.minihttp.util.BufferPool.BufferPool;
import com.minihttp.util.Pair.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Zzz {
    private final int port;
    private final AsynchronousChannelGroup channelGroup;
    private final ExecutorService executor;
    private final Router router;
    private final BufferPool bufferPool;
    private volatile boolean isRunning;

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
        String connectionHeaders = req.getHeader("connection");
        return (connectionHeaders != null) && Stream.of(connectionHeaders.split(";")).anyMatch(x -> x.equalsIgnoreCase("Keep-Alive"));
    }

    public void handleAccept(AsynchronousServerSocketChannel listener) {
        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                if (isRunning) {
                    listener.accept(null, this);
                    LogWrapper.log("[+] Accepted Connection");
                    handleClient(clientChannel);
                } else {
                    try {
                        clientChannel.close();
                    } catch (IOException e) {
                        LogWrapper.log("Failed to close client channel: " + e.getMessage());
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                LogWrapper.log("[-] Failed to accept connection: " + exc.getMessage());
            }
        });
    }

    public void start() throws IOException {
        AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(channelGroup);
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        listener.bind(address);
        isRunning = true;
        handleAccept(listener);
    }

    public Pair<HttpStatus, Pair<Boolean, HttpResponse>> requestHandler(String reqData) {
        Pair<HttpStatus, HttpRequest> req_status_handler = HttpRequest.Create.processHttpRequest(reqData);
        HttpRequest req = req_status_handler.getValue();
        Boolean isAlive = isKeepAliveRequested(req);
        if (req_status_handler.getKey() != HttpStatus.OK) {
            return new Pair<>(req_status_handler.getKey(), new Pair<>(isAlive, null));
        }
        Pair<PathParameters, HttpHandler> xv = this.router.find(req.getURI(), req.getHttpMethod());
        if (xv != null) {

            HttpHandler handler = xv.getValue();
            PathParameters kv = xv.getKey();
            try {
                HttpResponse r = handler.handle(req, kv);
                return new Pair<>(req_status_handler.getKey(), new Pair<>(isAlive, r));
            } catch (Exception e) {
                LogWrapper.log(e.getMessage());
                return new Pair<>(HttpStatus.INTERNAL_SERVER_ERROR, new Pair<>(isAlive, null));
            }
        } else {
            return new Pair<>(HttpStatus.NOT_FOUND, new Pair<>(isAlive, null));
        }
    }

    private void handleClient(AsynchronousSocketChannel clientChannel) {
        ByteBuffer buffer = bufferPool.acquireBuffer();
        handleRead(clientChannel, buffer);
    }

    private void handleRead(AsynchronousSocketChannel clientChannel, ByteBuffer buffer) {
        clientChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {

            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead > 0) {
                    buffer.flip();
                    String message = StandardCharsets.UTF_8.decode(buffer).toString();
                    Pair<HttpStatus, Pair<Boolean, HttpResponse>> response;
                    try {
                        response = requestHandler(message);
                    } catch (Exception e) {
                        LogWrapper.log("[-] " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    ByteBuffer responseBuffer;
                    if (response.getValue().getValue() == null) {
                        responseBuffer = StandardCharsets.UTF_8.encode(createErrorResponse(response.getKey()).toString());
                    } else {
                        responseBuffer = StandardCharsets.UTF_8.encode(response.getValue().getValue().toString());
                    }

                    Boolean keepAlive = response.getValue().getKey();
                    handleWrite(clientChannel, responseBuffer, keepAlive, buffer);
                } else {
                    // No more data to read, do not close the channel yet
                    buffer.clear();
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                LogWrapper.log("Failed to read from client: " + exc.getMessage());
                try {
                    bufferPool.release(buffer);
                    clientChannel.close();
                } catch (IOException e) {
                    LogWrapper.log("Failed to close client channel: " + e.getMessage());
                }
            }
        });
    }

    private void handleWrite(AsynchronousSocketChannel clientChannel, ByteBuffer responseBuffer, boolean keepAlive, ByteBuffer buffer) {
        clientChannel.write(responseBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesWritten, Void attachment) {
                if (bytesWritten > 0) {
                    if (responseBuffer.hasRemaining()) {
                        handleWrite(clientChannel, responseBuffer, keepAlive, buffer);
                    } else {
                        if (!keepAlive) {
                            try {
                                bufferPool.release(buffer);
                                clientChannel.close();
                                LogWrapper.log("Closing client");
                            } catch (IOException e) {
                                LogWrapper.log("Failed to close client channel: " + e.getMessage());
                            }
                        } else {
                            buffer.clear();
                            handleRead(clientChannel, buffer);
                        }
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
                    LogWrapper.log("Failed to close client channel: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown() throws IOException {
        if (!isRunning) {
            throw new IllegalStateException("Server is not running.");
        }
        isRunning = false;
        channelGroup.shutdown();
        executor.shutdown();

        try {
            channelGroup.awaitTermination(5, TimeUnit.SECONDS);
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogWrapper.log("Failed to gracefully shutdown: " + e.getMessage());
        }
        LogWrapper.log("Successfully shutting down");
    }
}
