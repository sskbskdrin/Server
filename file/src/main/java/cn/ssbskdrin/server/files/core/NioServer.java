package cn.ssbskdrin.server.files.core;

import java.io.IOException;
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

import cn.ssbskdrin.server.files.Log;
import cn.ssbskdrin.server.files.http.HttpHandler;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class NioServer {
    public static void main(String[] args) {
        final Thread thread = new Thread(() -> bind(8080).channelContextProvider(HttpHandler::new).build().start());
        thread.start();
        try {
            Thread.sleep(24 * 2600 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            thread.interrupt();
        }
    }

    private final ChannelContextProvider channelContextProvider;
    private final Queue<ChannelContext> queue = new SynchronousQueue<>(true);

    private final int port;
    Executor executor = Executors.newFixedThreadPool(3);

    private NioServer(Builder builder) {
        port = builder.port;
        channelContextProvider = builder.provider;
    }

    void start() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            Selector selector = Selector.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            loop(selector);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void loop(Selector selector) {
        for (; ; ) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
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
            } catch (IOException e) {
                e.printStackTrace();
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
