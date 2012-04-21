package spullara.nio.channels;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.functions.*;

import org.junit.*;

import spullara.util.concurrent.*;

public class FutureSocketChannelTest {
    @Test public void testEchoServer() throws Exception {
	final AtomicReference<String> result = new AtomicReference<String>();
	final CountDownLatch latch = new CountDownLatch(1);
	SocketAddress serverSocket = new InetSocketAddress(8000);
	SocketAddress clientSocket = new InetSocketAddress("localhost", 8000);
	final FutureServerSocketChannel fssc = new FutureServerSocketChannel().bind(serverSocket);
	Block<FutureSocketChannel> accepted = new Block<FutureSocketChannel>() {
	    public void apply(FutureSocketChannel fsc) {
		fssc.accept().onSuccess(this);
		ByteBuffer bb = ByteBuffer.allocate(1024);
		fsc.read(bb).onSuccess(length -> {
			bb.flip();
			fsc.write(bb);
			fsc.close();
		    });
	    }
	};
	fssc.accept().onSuccess(accepted);
	FutureSocketChannel fsc = new FutureSocketChannel();
	fsc.connect(clientSocket).onSuccess( v -> {
		fsc.write(ByteBuffer.wrap("hello".getBytes())).onSuccess( sent -> {
			ByteBuffer readBuffer = ByteBuffer.allocate(sent);
			fsc.read(readBuffer).onSuccess( recv -> {
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