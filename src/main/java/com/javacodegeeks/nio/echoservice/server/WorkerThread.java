package com.javacodegeeks.nio.echoservice.server;

import com.javacodegeeks.nio.echoservice.common.ChannelWriter;
import com.javacodegeeks.nio.echoservice.common.Constants;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class WorkerThread implements Runnable, ChannelWriter {
    
    private final Map<SocketChannel, StringBuilder> session;
//    private ServerSocketChannel channel;
//    private Set<SelectionKey> keys;

    private SelectionKey key;

    private static final int BUFFER_SIZE = 1024;
    
//    public WorkerThread(final Map<SocketChannel, StringBuilder> session, final ServerSocketChannel channel, final Set<SelectionKey> keys) {
//        this.session = session;
//        this.channel = channel;
//        this.keys = keys;
//    }

    public WorkerThread(final Map<SocketChannel, StringBuilder> session, final SelectionKey key) {
        this.session = session;
        this.key = key;
    }
    
    @Override
    public void run() {
//        assert !Objects.isNull(keys) && !Objects.isNull(channel);
//
//        final Iterator<SelectionKey> iterator = keys.iterator();
//
//        while (iterator.hasNext()) {
//            try {
//                final SelectionKey key = iterator.next();
//
//                try {
//                    if (key.isValid()) {
//                        if (key.isAcceptable()) {
//                            logger.info("doAccept");
//                            doAccept(channel, key);
//                        } else if (key.isReadable()) {
//                            logger.info("doProcess");
//                            doProcess(key);
//                        } else {
//                            throw new UnsupportedOperationException("Key not supported by server.");
//                        }
//                    } else {
//                        throw new UnsupportedOperationException("Key not valid.");
//                    }
//                } finally {
//                    iterator.remove();
//                }
//            } catch (IOException e) {
//                logger.error(e.getMessage());
//            }
//        }

        try {
            doProcess(key);
        } catch (IOException e) {
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

//    private void doAccept(final ServerSocketChannel channel, final SelectionKey key) throws IOException {
//        assert !Objects.isNull(key) && !Objects.isNull(channel);
//
//        final SocketChannel client = channel.accept();
//        client.configureBlocking(false);
//        client.register(key.selector(), SelectionKey.OP_READ);
//
//        // Create a session for the incoming connection
//        this.session.put(client, new StringBuilder());
//    }

    private void doProcess(final SelectionKey key) throws IOException {
        assert !Objects.isNull(key);

        final SocketChannel client = (SocketChannel) key.channel();
        final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        final int bytesRead = client.read(buffer);
        if (bytesRead > 0) {
            String message = new String(buffer.array()).trim();
//            logger.info("read: "+message);

            this.session.get(client).append(message.trim());
            doEcho(message);
        } else if (bytesRead < 0) {
            cleanUp(key);
        }

//        if (mustEcho(key)) {
//            doEcho(key);
//        }
    }

    private void doEcho(final String message) throws IOException {
//        logger.info("write: "+message);

        final ByteBuffer buffer = ByteBuffer.wrap(message.trim().getBytes());

        doWrite(buffer, (SocketChannel) key.channel());
    }

    private void doEcho(final SelectionKey key) throws IOException {
        assert !Objects.isNull(key);

        StringBuilder sb = this.session.get(key.channel());
        String message = sb.toString();
//        logger.info("write: "+message);

        final ByteBuffer buffer = ByteBuffer.wrap(message.trim().getBytes());

        doWrite(buffer, (SocketChannel) key.channel());
        sb.delete(0, sb.length());
    }

    private boolean mustEcho(final SelectionKey key) {
        assert !Objects.isNull(key);

        if (key.channel() instanceof SocketChannel) {
            StringBuilder sb = this.session.get(key.channel());
            if (sb != null) {
                return sb.toString().contains(Constants.END_MESSAGE_MARKER);
            }
        }
        return false;
    }

    private void cleanUp(final SelectionKey key) throws IOException {
        assert !Objects.isNull(key);

        this.session.remove(key.channel());

        key.channel().close();
        key.cancel();
    }

}
