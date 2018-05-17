package cn.sskbskdrin.server.socket;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

    public static void main(String[] args) {
        try {
            start(8888);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup boss = new NioEventLoopGroup();
                EventLoopGroup worker = new NioEventLoopGroup();
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(boss, worker).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress
                            (port)).childHandler(new ChannelInitializer<SocketChannel>() {
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
                    System.out.println("sync");
                    f.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            System.out.println("operationComplete:" + channelFuture);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //关闭EventLoopGroup，释放资源
                    boss.shutdownGracefully();
                    worker.shutdownGracefully();
                }
                System.out.println("netty socket server end");
            }
        }).start();
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
