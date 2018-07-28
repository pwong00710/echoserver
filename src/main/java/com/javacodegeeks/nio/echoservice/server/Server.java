package com.javacodegeeks.nio.echoservice.server;

import com.javacodegeeks.nio.echoservice.common.ChannelWriter;
import com.javacodegeeks.nio.echoservice.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public final class Server implements ChannelWriter {

    private static final int BUFFER_SIZE = 1024;

    private final int port;
    private final Map<SocketChannel, StringBuilder> session;

    private int corePoolSize = 4;
    private int maxPoolSize = 4;
    private long keepAliveTime = 5000;

//    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
//    private final ExecutorService executor = new ThreadPoolExecutor(
//                                            corePoolSize,
//                                            maxPoolSize,
//                                            keepAliveTime,
//                                            TimeUnit.MILLISECONDS,
//                                            new ArrayBlockingQueue<>(1000));

    public static void main(final String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Expecting one argument (1) port.");
        }

        new Server(Integer.valueOf(args[0])).start();
    }

    private Server(final int port) {
        this.port = port;
        this.session = new ConcurrentHashMap<>();
    }

    private void start() {
        try (Selector selector = Selector.open(); ServerSocketChannel channel = ServerSocketChannel.open()) {
            initChannel(channel, selector);

            while (!Thread.currentThread().isInterrupted()) {
                if (selector.isOpen()) {
                    final int numKeys = selector.select();
//                    log.info("numKeys="+numKeys);
                    if (numKeys > 0) {
                        try {
                            Set<SelectionKey> copiedSet = new HashSet<>(selector.selectedKeys());
                            handleKeys(channel, copiedSet);
                        } finally {
                            selector.selectedKeys().clear();
                        }
                    }
                } else {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to start server.", e);
        } finally {
            this.session.clear();
        }
    }

    private void initChannel(final ServerSocketChannel channel, final Selector selector) throws IOException {
        assert !Objects.isNull(channel) && !Objects.isNull(selector);

        channel.socket().setReuseAddress(true);
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(this.port));
        channel.register(selector, SelectionKey.OP_ACCEPT);
        
        log.info("Server is ready!");
    }

    private void handleKeys(final ServerSocketChannel channel, final Set<SelectionKey> keys) {
        assert !Objects.isNull(keys) && !Objects.isNull(channel);

//        executor.execute(new WorkerThread(session, channel, keys));
        final Iterator<SelectionKey> iterator = keys.iterator();

        while (iterator.hasNext()) {
            try {
                final SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
//                            log.info("doAccept");
                            doAccept(channel, key);
                        } else if (key.isReadable()) {
//                            log.info("doProcess");
//                            doProcess(key);
                            executor.execute(new WorkerThread(session, key));
                        } else {
                            throw new UnsupportedOperationException("Key not supported by server.");
                        }
                    } else {
                        throw new UnsupportedOperationException("Key not valid.");
                    }
                } finally {
//                    iterator.remove();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
//    private void doWrite(final SelectionKey key) throws IOException {
//        if (mustEcho(key)) {
//            doEcho(key);
//        }        
//    }

    private void doAccept(final ServerSocketChannel channel, final SelectionKey key) throws IOException {
        assert !Objects.isNull(key) && !Objects.isNull(channel);

        final SocketChannel client = channel.accept();
        client.configureBlocking(false);
        client.register(key.selector(), SelectionKey.OP_READ);

        // Create a session for the incoming connection
        this.session.put(client, new StringBuilder());
    }

//    private void doProcess(final SelectionKey key) throws IOException {
//        assert !Objects.isNull(key);
//
//        final SocketChannel client = (SocketChannel) key.channel();
//        final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
//
//        final int bytesRead = client.read(buffer);
//        if (bytesRead > 0) {
//            String message = new String(buffer.array()).trim();
////            log.info("read: "+message);
//
//            this.session.get(client).append(message.trim());
//        } else if (bytesRead < 0) {
//            cleanUp(key);
//        }
//
//        if (mustEcho(key)) {
//            doEcho(key);
//        }
//    }
//
//    private void doEcho(final SelectionKey key) throws IOException {
//        assert !Objects.isNull(key);
//
//        StringBuilder sb = this.session.get(key.channel());
//        String message = sb.toString();
////        log.info("write: "+message);
//
//        final ByteBuffer buffer = ByteBuffer.wrap(message.trim().getBytes());
//
//        doWrite(buffer, (SocketChannel) key.channel());
//        sb.delete(0, sb.length());
//    }
//
//    private boolean mustEcho(final SelectionKey key) {
//        assert !Objects.isNull(key);
//
//        if (key.channel() instanceof SocketChannel) {
//            StringBuilder sb = this.session.get(key.channel());
//            if (sb != null) {
//                return sb.toString().contains(Constants.END_MESSAGE_MARKER);
//            }
//        }
//        return false;
//    }
//
//    private void cleanUp(final SelectionKey key) throws IOException {
//        assert !Objects.isNull(key);
//
//        this.session.remove(key.channel());
//
//        key.channel().close();
//        key.cancel();
//    }
}
