package spullara.nio.channels;

import spullara.util.concurrent.Promise;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

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

    public Promise<Void> connect(SocketAddress sa) {
        Promise<Void> connector = new Promise<>();
        asc.connect(sa, connector, new CompletionHandler<Void, Promise<Void>>() {
            public void completed(Void v1, Promise<Void> connector) {
                connector.set(null);
            }

            public void failed(Throwable th, Promise<Void> connector) {
                connector.setException(th);
            }
        });
        return connector;
    }

    public Promise<Integer> read(ByteBuffer buffer) {
        Promise<Integer> reader = new Promise<>();
        asc.read(buffer, reader, new CompletionHandler<Integer, Promise<Integer>>() {
            public void completed(Integer length, Promise<Integer> reader) {
                reader.set(length);
            }

            public void failed(Throwable th, Promise<Integer> reader) {
                reader.setException(th);
            }
        });
        return reader;
    }

    public Promise<Integer> write(ByteBuffer buffer) {
        Promise<Integer> writer = new Promise<>();
        asc.write(buffer, writer, new CompletionHandler<Integer, Promise<Integer>>() {
            public void completed(Integer length, Promise<Integer> writer) {
                writer.set(length);
            }

            public void failed(Throwable th, Promise<Integer> writer) {
                writer.setException(th);
            }
        });
        return writer;
    }
}
