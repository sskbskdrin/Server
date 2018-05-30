package cn.sskbskdrin.server.ftp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import cn.sskbskdrin.log.Logger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author ex-keayuan001
 */
public class FtpDataService {
    private static final String TAG = "FtpDataService";

    private ServiceThread mThread;

    private static List<ChannelHandlerContext> mChannels;

    private static FtpDataService mInstance;

    private FtpDataService() {
        mChannels = new ArrayList<>(5);
    }

    public static FtpDataService getInstance() {
        if (mInstance == null) {
            synchronized (FtpDataService.class) {
                if (mInstance == null) {
                    mInstance = new FtpDataService();
                }
            }
        }
        return mInstance;
    }

    public static void main(String[] args) {
        try {
            getInstance().start(8888);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(final int port) {
        System.out.println("start port=" + port);
        if (mThread == null) {
            stop();
            mThread = new ServiceThread(port);
            mThread.start();
        }
    }

    public int getPort() {
        if (mThread == null) {
            start(62121);
        }
        return 62121;
    }

    public void stop() {
        if (mThread != null) {
            mThread.stopService();
        }
        mThread = null;
    }


    public static ChannelHandlerContext getSocket(SocketAddress address) {
        Logger.d(TAG, "getSocket address=" + address.toString());
        synchronized (DataSocketController.class) {
            for (int i = 0; i < mChannels.size(); i++) {
                SocketAddress temp = mChannels.get(i).channel().remoteAddress();
                if (temp != null && temp.equals(address)) {
                    return mChannels.remove(i);
                }
            }
        }
        return null;
    }

    private class ServiceThread extends Thread {

        private int mPort;
        private Channel mChannel;

        ServiceThread(int port) {
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
            EventLoopGroup boss = new NioEventLoopGroup();
            EventLoopGroup worker = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(boss, worker).channel(NioServerSocketChannel.class).localAddress(new
                        InetSocketAddress(mPort)).childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        System.out.println("initChannel");
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("decoder", new FtpDecoder());
                        pipeline.addLast("encoder", new FtpEncoder());
                        pipeline.addLast("handler", new ServiceHandler());
                    }
                }).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark
                        (1024, 32 * 1024));
                //绑定监听
                ChannelFuture f = b.bind().sync();
                System.out.println("正在监听...");
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //关闭EventLoopGroup，释放资源
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
            System.out.println("netty socket cn.sskbskdrin.server end");
        }
    }

    private static class ServiceHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            mChannels.add(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
