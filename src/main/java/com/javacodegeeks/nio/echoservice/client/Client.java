package com.javacodegeeks.nio.echoservice.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.javacodegeeks.nio.echoservice.common.ChannelWriter;
import com.javacodegeeks.nio.echoservice.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

@Slf4j
public final class Client implements ChannelWriter {
    
    private final InetSocketAddress hostAddress;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
//    private final AtomicInteger remainingClients = new AtomicInteger(0);

    public static void main(final String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("Expecting two arguments in order (1) port (2) message eg: 9999 \"Hello world\" (3) loop count (4) no. of client.");
        }
        
        int port = Integer.valueOf(args[0]);
        String message = args[1];
        int loopCount = Integer.parseInt(args[2]);
        int noOfClient = Integer.parseInt(args[3]);
        
        Client client = new Client(port);

        client.log.info("Start time "+LocalDateTime.now());
        
        IntStream.range(0, noOfClient).forEach(i -> client.start(i, message, loopCount));
        
        client.waitForTermination();

        client.log.info("End time "+LocalDateTime.now());
    }

    private Client(final int port) {
        this.hostAddress = new InetSocketAddress(port);
    }

    private void start(final int clientId, final String message, final int count) {
        assert StringUtils.isNotEmpty(message);

//        remainingClients.incrementAndGet();
        
        Runnable runnableTask = () -> {
            try {
                SocketChannel client = SocketChannel.open(this.hostAddress);
                        
                client.configureBlocking(false);
                
                for (int i=0;i<count;i++) {

                    LocalDateTime currentTime = LocalDateTime.now();

                    String request = currentTime + "-[" + clientId + "]: " + message;

                    final ByteBuffer buffer = ByteBuffer.wrap((request + Constants.END_MESSAGE_MARKER).trim().getBytes());

//                    logger.info("doWrite");
                    this.doWrite(buffer, client);

                    buffer.flip();

                    final StringBuilder echo = new StringBuilder();
//                    logger.info("doRead");
                    this.doRead(echo, buffer, client);
                    
                    String response = echo.toString();

                    response = response.replace(Constants.END_MESSAGE_MARKER, StringUtils.EMPTY);

//                    logger.info(String.format("Message :\t %s \nEcho    :\t %s", request, response));

                    Assert.assertEquals(request, response);
                }
            } catch (IOException e) {
                log.error("Unable to communicate with server.", e);
            } catch (AssertionError e) {
                log.error("Assertion error", e);                
            } finally {
                log.info("Client"+ clientId + " exit!!!");
//                shutdown();
            }
        };

        executor.execute(runnableTask);
    }
    
    private void waitForTermination() throws InterruptedException {
        executor.shutdown();
        while(!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
        }
    }
    
//    private void shutdown() {
//        if (remainingClients.decrementAndGet() == 0) {
//            executor.shutdown();
//        }
//    }

    private void doRead(final StringBuilder data, final ByteBuffer buffer, final SocketChannel channel) throws IOException {
        assert !Objects.isNull(data) && !Objects.isNull(buffer) && !Objects.isNull(channel);

        while (true) {
            final int bytesRead = channel.read(buffer);
            if (bytesRead > 0) {
                data.append(new String(buffer.array()).trim());
            }
            buffer.clear();
            if (data.toString().contains(Constants.END_MESSAGE_MARKER)) {
                break;
            }
        }
    }
}
