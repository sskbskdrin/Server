package cn.sskbskdrin.server.socket;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class NettyServer {

    private SocketThread mThread;

    private static NettyServer mInstance;

    private NettyServer() {
    }

    public static NettyServer getInstance() {
        if (mInstance == null) {
            synchronized (NettyServer.class) {
                if (mInstance == null) {
                    mInstance = new NettyServer();
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
        stop();
        mThread = new SocketThread(port);
        mThread.start();
    }

    public void stop() {
        if (mThread != null) {
            mThread.stopService();
        }
        mThread = null;
    }

    private class SocketThread extends Thread {

        private int mPort;
        private Channel mChannel;

        SocketThread(int port) {
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
                b.group(boss, worker).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(mPort)
                ).childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        System.out.println("initChannel");
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("decoder", new BodyDecoder());
                        pipeline.addLast("encoder", new BodyEncoder());
                        pipeline.addLast("handler", new ServiceHandler());
                    }
                }).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024, 32 * 1024));
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

    private static class ServiceHandler extends EchoHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Body) {
                System.out.println("Client:" + msg.toString());
                Body body = (Body) msg;
                body.head = 0xBBCC;
                body.flag = 0x3300;
                body.data = "I am service".getBytes();
                body.crc = 0x30;
                System.out.println("Service:" + msg.toString());
                ctx.writeAndFlush(body);
            } else if (msg instanceof ByteBuf) {
                ByteBuf in = (ByteBuf) msg;
                System.out.println("channelRead:" + in.toString(CharsetUtil.UTF_8));
            } else {
                System.out.println(msg.toString());
            }
        }
    }
}
