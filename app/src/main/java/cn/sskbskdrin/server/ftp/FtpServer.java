package cn.sskbskdrin.server.ftp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
import io.netty.handler.codec.ByteToMessageDecoder;

public class FtpServer {
    private FtpThread mThread;

    private static FtpServer mInstance;

    private FtpServer() {
    }

    public static FtpServer getInstance() {
        if (mInstance == null) {
            synchronized (FtpServer.class) {
                if (mInstance == null) {
                    mInstance = new FtpServer();
                }
            }
        }
        return mInstance;
    }

    public static void main(String[] args) {
        FtpServer.getInstance().start(2100);
        //        try {
        //            new FtpServer().listen(2100);
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
    }

    public void listen(int port) throws IOException {
        Socket socket = null;
        Share.init();
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            //这个是建立连接,三次握手的过程，当连接建立了之后，两个socket之间的通讯是直接通过流进行的，不用再通过这一步
            socket = serverSocket.accept();
            ControllerThread thread = new ControllerThread(socket);
            thread.start();
        }
    }

    public void start(final int port) {
        System.out.println("start port=" + port);
        stop();
        mThread = new FtpThread(port);
        mThread.start();
    }

    public void stop() {
        if (mThread != null) {
            mThread.stopService();
        }
        mThread = null;
    }

    private class FtpThread extends Thread {

        private int mPort;
        private Channel mChannel;

        FtpThread(int port) {
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
            EventLoopGroup boss = new NioEventLoopGroup();
            EventLoopGroup worker = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(boss, worker).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(mPort)
                ).childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.write("220 hello ftp\r\n");
                        socketChannel.flush();
                        System.out.println("initChannel");
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addFirst("decoder", new ByteToMessageDecoder() {
                            @Override
                            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                                System.out.println("decoder");
                                if (in == null) {
                                    return;
                                }
                                int ableLength = in.readableBytes();
                                byte[] data = new byte[ableLength];
                                in.readBytes(data);
                                out.add(new String(data));
                            }
                        });
                        pipeline.addLast("handler", new FtpHandler());
                    }
                }).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024, 32 * 1024));
                //绑定监听
                ChannelFuture f = b.bind().sync();
                System.out.println("正在监听...");
                mChannel = f.channel();
                mChannel.closeFuture().sync();
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
            System.out.println("http service end");
        }
    }
}
