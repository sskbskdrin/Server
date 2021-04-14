package cn.ssbskdrin.server.files.core;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

import cn.ssbskdrin.server.files.util.IOUtils;
import cn.ssbskdrin.server.files.util.Log;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class NioServer {

    private final ChannelContextProvider channelContextProvider;
    private final Queue<ChannelContext> queue = new SynchronousQueue<>(true);

    private final int port;
    private volatile Thread wordThread;
    private volatile boolean isWorking;
    private final Executor executor = Executors.newFixedThreadPool(2);

    private NioServer(Builder builder) {
        port = builder.port;
        channelContextProvider = builder.provider;
    }

    public void sync() {
        try {
            start();
            for (; ; ) {
                synchronized (this) {
                    this.wait(3000);
                }
                if (!isWorking) {
                    if (wordThread != null) {
                        wordThread.interrupt();
                    }
                    while (wordThread != null) {
                        Thread.yield();
                    }
                    start();
                }
                isWorking = false;
            }
        } catch (InterruptedException e) {
            if (wordThread != null) {
                wordThread.interrupt();
            }
        }
    }

    public void async() {
        executor.execute(this::async);
    }

    private void start() {
        executor.execute(() -> {
            Log.i("work thread start");
            wordThread = Thread.currentThread();
            try {
                loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            wordThread = null;
            Log.w("work thread interrupted");
        });
    }

    private void loop() {
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            Selector selector = Selector.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (!Thread.currentThread().isInterrupted()) {
                isWorking = true;
                if (selector.select(2000) == 0) {
                    Log.d("wait==");
                    continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        ChannelContext context = channelContextProvider.getChannelContext();
                        context.setKey(socketChannel.register(key.selector(), SelectionKey.OP_READ, context));
                    } else if (key.isReadable()) {
                        ChannelContext context = (ChannelContext) key.attachment();
                        context.read();
                    } else if (key.isValid() && key.isWritable()) {
                        ChannelContext context = (ChannelContext) key.attachment();
                        context.write();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(e.getMessage(), e);
        } finally {
            if (serverSocketChannel != null) {
                IOUtils.closeQuietly(serverSocketChannel);
            }
        }
    }

    public void remove(ChannelContext channelContext) {
        queue.remove(channelContext);
    }

    public interface ChannelContextProvider {
        ChannelContext getChannelContext();
    }

    public static Builder bind(int port) {
        return new Builder(port);
    }

    public static class Builder {
        private final int port;
        private ChannelContextProvider provider;

        Builder(int port) {
            this.port = port;
        }

        public Builder channelContextProvider(ChannelContextProvider provider) {
            this.provider = provider;
            return this;
        }

        public NioServer build() {
            return new NioServer(this);
        }
    }

}
