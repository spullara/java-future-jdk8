package spullara.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class FutureSocketChannel {

    private AsynchronousSocketChannel asc;

    public FutureSocketChannel() throws IOException {
        this.asc = AsynchronousSocketChannel.open();
    }

    protected FutureSocketChannel(AsynchronousSocketChannel asc) {
        this.asc = asc;
    }

    public void close() {
        try {
            asc.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    public CompletableFuture<Void> connect(SocketAddress sa) {
        CompletableFuture<Void> connector = new CompletableFuture<>();
        asc.connect(sa, connector, new CompletionHandler<Void, CompletableFuture<Void>>() {
            public void completed(Void v1, CompletableFuture<Void> connector) {
                connector.complete(null);
            }

            public void failed(Throwable th, CompletableFuture<Void> connector) {
                connector.completeExceptionally(th);
            }
        });
        return connector;
    }

    public CompletableFuture<Integer> read(ByteBuffer buffer) {
        CompletableFuture<Integer> reader = new CompletableFuture<>();
        asc.read(buffer, reader, new CompletionHandler<Integer, CompletableFuture<Integer>>() {
            public void completed(Integer length, CompletableFuture<Integer> reader) {
                reader.complete(length);
            }

            public void failed(Throwable th, CompletableFuture<Integer> reader) {
                reader.completeExceptionally(th);
            }
        });
        return reader;
    }

    public CompletableFuture<Integer> write(ByteBuffer buffer) {
        CompletableFuture<Integer> writer = new CompletableFuture<>();
        asc.write(buffer, writer, new CompletionHandler<Integer, CompletableFuture<Integer>>() {
            public void completed(Integer length, CompletableFuture<Integer> writer) {
                writer.complete(length);
            }

            public void failed(Throwable th, CompletableFuture<Integer> writer) {
                writer.completeExceptionally(th);
            }
        });
        return writer;
    }

    public int getPort() throws IOException {
        return ((InetSocketAddress)asc.getLocalAddress()).getPort();
    }

    public static FutureSocketChannel open() throws IOException {
        return new FutureSocketChannel(AsynchronousSocketChannel.open());
    }

    public static FutureSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return new FutureSocketChannel(AsynchronousSocketChannel.open(group));
    }
}
