package cn.sskbskdrin.server.ftp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class FtpHandler extends ChannelInboundHandlerAdapter {
    private static final String TAG = "FtpHandler";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log("channelActive");
        ctx.write("220 hello ftp\r\n");
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log("channelRead:" + msg.toString());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log("channelReadComplete:");
        ctx.flush();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelWritabilityChanged:" + ctx.channel().isWritable());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static void log(String log) {
        System.out.println(TAG + ": " + log);
    }

}
