package cn.ssbskdrin.server.files;

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

import cn.ssbskdrin.server.files.http.HttpHandler;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class NioServer implements Log {
    public static void main(String[] args) {
        bind(8080).channelContextProvider(HttpHandler::new).build().start();
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
                    log("wait==");
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
                        ChannelContext context = channelContextProvider.getChannelContext(socketChannel);
                        context.setServer(this);
                        queue.offer(context);
                        socketChannel.register(key.selector(), SelectionKey.OP_READ, context);
                    } else if (key.isReadable()) {
                        ChannelContext channelContext = (ChannelContext) key.attachment();
                        if (channelContext.tryRead()) {
                            executor.execute(channelContext);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while (!queue.isEmpty()) {
            ChannelContext context = queue.poll();
            context.onException(new InterruptedException());
            context.onClose();
        }
    }

    public void remove(ChannelContext channelContext) {
        queue.remove(channelContext);
    }

    public interface ChannelContextProvider {
        ChannelContext getChannelContext(SocketChannel channel);
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
