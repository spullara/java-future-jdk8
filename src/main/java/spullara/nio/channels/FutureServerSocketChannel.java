package spullara.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class FutureServerSocketChannel {

    private AsynchronousServerSocketChannel assc;

    public FutureServerSocketChannel() throws IOException {
        this.assc = AsynchronousServerSocketChannel.open();
    }

    protected FutureServerSocketChannel(AsynchronousServerSocketChannel assc) throws IOException {
        this.assc = assc;
    }

    public FutureServerSocketChannel bind(SocketAddress sa) throws IOException {
        return new FutureServerSocketChannel(assc.bind(sa));
    }

    public CompletableFuture<FutureSocketChannel> accept() {
        final CompletableFuture<FutureSocketChannel> acceptor = new CompletableFuture<>();
        assc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel channel, Void v) {
                acceptor.complete(new FutureSocketChannel(channel));
            }

            public void failed(Throwable th, Void v) {
                acceptor.completeExceptionally(th);
            }
        });
        return acceptor;
    }

    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) assc.getLocalAddress();
    }

    public static FutureServerSocketChannel open() throws IOException {
        return new FutureServerSocketChannel(AsynchronousServerSocketChannel.open());
    }

    public static FutureServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return new FutureServerSocketChannel(AsynchronousServerSocketChannel.open(group));
    }
}
