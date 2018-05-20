package cn.sskbskdrin.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServer {
    private HttpThread mThread;

    private static HttpServer mInstance;

    private HttpServer() {
    }

    public static HttpServer getInstance() {
        if (mInstance == null) {
            synchronized (HttpServer.class) {
                if (mInstance == null) {
                    mInstance = new HttpServer();
                }
            }
        }
        return mInstance;
    }

    public void start(final int port) {
        System.out.println("start port=" + port);
        stop();
        mThread = new HttpThread(port);
        mThread.start();
    }

    public void stop() {
        if (mThread != null) {
            mThread.stopService();
        }
        mThread = null;
    }

    private class HttpThread extends Thread {

        private int mPort;
        private Channel mChannel;

        HttpThread(int port) {
            mPort = port;
        }

        public void stopService() {
            if (mChannel != null) {
                mChannel.close();
            }
            interrupt();
        }

        @Override
        public void run() {
            System.out.println("http server start");
            ServerBootstrap b = new ServerBootstrap();
            NioEventLoopGroup group = new NioEventLoopGroup();
            b.group(group).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    System.out.println("initChannel ch:" + ch);
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("decoder", new HttpRequestDecoder());
                    pipeline.addLast("encoder", new HttpResponseEncoder());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024));
                    pipeline.addLast("handler", new HttpHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued
                    .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
            System.out.println("bind");
            try {
                mChannel = b.bind(mPort).sync().channel();
                mChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("InterruptedException end");
            } finally {
                group.shutdownGracefully();
                System.out.println("Http finally");
            }
            System.out.println("http service end");
        }
    }
}
