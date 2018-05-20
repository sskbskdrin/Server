package cn.sskbskdrin.server.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class EchoHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("220 test regist\r\n");
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log("channelActive");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ctx.writeAndFlush("220 test echo\r\n");
            }
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Body) {
            log("channelRead:" + msg.toString());
        } else {
            ByteBuf in = (ByteBuf) msg;
            log("channelRead:" + in.toString(CharsetUtil.UTF_8));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log("channelReadComplete:");
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        System.out.println("channelWritabilityChanged:" + ctx.channel().isWritable());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static void log(String log) {
        System.out.println(log);
    }

}
