package cn.sskbskdrin.server.base;

import cn.sskbskdrin.server.util.SLog;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author sskbskdrin
 * @date 2019/April/23
 */
public abstract class BaseServer {
    protected static String TAG = "BaseServer";
    private ServerThread mThread;

    public final void start(final int port) {
        SLog.d(TAG, "server start port=" + port);
        if (mThread != null && mThread.isAlive()) {
            SLog.w(TAG, "server already start");
            return;
        }
        mThread = new ServerThread(this, port);
        mThread.start();
        onStart();
    }

    protected void onStart() {
    }

    public final void stop() {
        SLog.d(TAG, "server stop");
        if (mThread != null) {
            mThread.stopService();
        }
        mThread = null;
        onStop();
    }

    protected void onStop() {
    }

    protected boolean runServer(ServerBootstrap bootstrap) {
        return false;
    }

    protected abstract void initChannel(Channel ch);

    private static class ServerThread extends Thread {

        private int mPort;
        private Channel mChannel;
        private BaseServer mServer;

        private ServerThread(BaseServer server, int port) {
            mPort = port;
            mServer = server;
        }

        public void stopService() {
            if (mChannel != null) {
                mChannel.close();
            }
            interrupt();
        }

        @Override
        public void run() {
            SLog.d(TAG, "start run server");
            ServerBootstrap b = new ServerBootstrap();
            if (!mServer.runServer(b)) {
                NioEventLoopGroup group = new NioEventLoopGroup();
                NioEventLoopGroup child = new NioEventLoopGroup();
                b.group(group, child);
                b.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        SLog.d(TAG, "initChannel ch:" + ch);
                        mServer.initChannel(ch);
                    }
                });
                b.option(ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued
                    .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
                try {
                    mChannel = b.bind(mPort).sync().channel();
                    mChannel.closeFuture().sync();
                } catch (InterruptedException e) {
                    SLog.e(TAG, "server interrupt ", e);
                } finally {
                    group.shutdownGracefully();
                    child.shutdownGracefully();
                    SLog.w(TAG, "server finally");
                }
            }
            SLog.d(TAG, "service end");
        }
    }
}
