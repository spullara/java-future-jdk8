package spullara.nio.channels;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FutureSocketChannelTest {
    @Test
    public void testEchoServer() throws Exception {
        final AtomicReference<String> result = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        SocketAddress serverSocket = new InetSocketAddress(0);
        final FutureServerSocketChannel fssc = new FutureServerSocketChannel().bind(serverSocket);
        Consumer<FutureSocketChannel> accepted = new Consumer<FutureSocketChannel>() {
            public void accept(FutureSocketChannel fsc) {
                fssc.accept().thenAccept(this);
                ByteBuffer bb = ByteBuffer.allocate(1024);
                fsc.read(bb).thenAccept(length -> {
                    bb.flip();
                    fsc.write(bb);
                    fsc.close();
                });
            }
        };
        fssc.accept().thenAccept(accepted);
        FutureSocketChannel fsc = new FutureSocketChannel();
        SocketAddress clientSocket = new InetSocketAddress("localhost", fssc.getLocalAddress().getPort());
        fsc.connect(clientSocket).thenAccept(v -> {
            fsc.write(ByteBuffer.wrap("hello".getBytes())).thenAccept(sent -> {
                ByteBuffer readBuffer = ByteBuffer.allocate(sent);
                fsc.read(readBuffer).thenAccept(recv -> {
                    readBuffer.flip();
                    result.set(new String(readBuffer.array()));
                    latch.countDown();
                });
            });
        });
        latch.await();
        Assert.assertEquals("hello", result.get());
    }
}