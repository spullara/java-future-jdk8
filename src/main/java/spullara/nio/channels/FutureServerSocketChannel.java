package spullara.nio.channels;

import spullara.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

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

    public Promise<FutureSocketChannel> accept() {
        final Promise<FutureSocketChannel> acceptor = new Promise<>();
        assc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel channel, Void v) {
                acceptor.set(new FutureSocketChannel(channel));
            }

            public void failed(Throwable th, Void v) {
                acceptor.setException(th);
            }
        });
        return acceptor;
    }

    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) assc.getLocalAddress();
    }
}
