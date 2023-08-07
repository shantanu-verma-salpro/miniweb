package com.minihttp.server.core;


import com.minihttp.LogWrapper.LogWrapper;
import com.minihttp.PathParameters.PathParameters;
import com.minihttp.handlers.HttpHandler.HttpHandler;
import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.http.HttpRequest.HttpRequest;
import com.minihttp.http.HttpResponse.HttpResponse;
import com.minihttp.http.HttpStatus.HttpStatus;
import com.minihttp.routing.Router.Router;
import com.minihttp.util.Pair.Pair;

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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

class EventLoop implements Runnable {
    final AtomicReference<Selector> selectorRef = new AtomicReference<>();

    public EventLoop() throws IOException {
        selectorRef.set(Selector.open());
    }

    @Override
    public void run() {
        Selector selector = selectorRef.get();
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    ((Runnable) selectionKey.attachment()).run();
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class MiniServer {
    static public Router router;
    private final EventLoop mainAcceptor;
    private final EventLoop[] workers;
    private final ExecutorService pool;
    private final ServerSocketChannel serverSocketChannel;


    public MiniServer() throws IOException {
        router = new Router();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(8090));
        int cpus = Runtime.getRuntime().availableProcessors() * 2;
        workers = new EventLoop[cpus - 1];
        for (int i = 0; i < cpus - 1; i++) {
            workers[i] = new EventLoop();
        }
        mainAcceptor = new EventLoop();
        pool = Executors.newFixedThreadPool(cpus);
        serverSocketChannel.register(mainAcceptor.selectorRef.get(), SelectionKey.OP_ACCEPT, new Acceptor());
    }

    public void addRoute(String uri, HttpMethod method, HttpHandler handler) throws URISyntaxException {
        this.router.add(uri, method, handler);
    }


    public void start() {
        pool.submit(mainAcceptor);
        for (EventLoop ev : workers) {
            pool.submit(ev);
        }
    }

    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();

                if (socketChannel != null) {
                    int workerIdx = socketChannel.hashCode() % workers.length;
                    EventLoop worker = workers[workerIdx];
                    new ReaderWriter(worker, socketChannel, router);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class ReaderWriter implements Runnable {
    private final SocketChannel socketChannel;
    private final SelectionKey key;
    private final Writer writer;
    private final ByteBuffer input = ByteBuffer.allocate(1024);
    Router router = null;
    ReaderWriter(EventLoop ev, SocketChannel socketChannel, Router router) throws IOException, InterruptedException {
        this.router = router;
        this.writer = new Writer();
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        ev.selectorRef.get().wakeup();
        key = socketChannel.register(ev.selectorRef.get(), 0);
        key.attach(this);
        key.interestOps(SelectionKey.OP_READ);
        key.selector().wakeup();
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

    @Override
    public void run() {
        try {
            if (socketChannel.isConnected()) {
                input.clear();
                int bytesRead = socketChannel.read(input);
                if (bytesRead == -1) {
                    closeChannel();
                    return;
                }
                input.flip();
                String message = StandardCharsets.UTF_8.decode(input).toString();
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
                writer.prepareWrite(responseBuffer);
            }
        } catch (IOException e) {
            writer.handleError(e);
        }
    }

    void closeChannel() {
        try {
            socketChannel.close();
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Writer implements Runnable {
        private ByteBuffer response = null;

        @Override
        public void run() {
            try {
                int bytesWritten = socketChannel.write(response);
                if (bytesWritten == -1) {
                    // An error occurred, handle it
                    handleError(new IOException("Error while writing to socket"));
                } else if (response.hasRemaining()) {
                    // There are remaining bytes to be written, register the channel for write readiness
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.selector().wakeup();
                    key.attach(this);
                } else {
                    // All bytes are written, move to the next state
                    closeChannel();
                }
            } catch (IOException e) {
                handleError(e);
            }
        }

        void prepareWrite(ByteBuffer response) {
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
            key.attach(this);
            this.response = response;
        }

        void handleError(IOException e) {
            e.printStackTrace();
            closeChannel();
            response = null;
        }
    }
}

