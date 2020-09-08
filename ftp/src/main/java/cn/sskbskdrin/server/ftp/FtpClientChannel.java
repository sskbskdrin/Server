package cn.sskbskdrin.server.ftp;

import cn.sskbskdrin.server.util.SLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author ex-keayuan001
 */
public class FtpClientChannel extends ChannelInboundHandlerAdapter {
    private static final String TAG = "FtpClientChannel";

    private Session session;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        SLog.d(TAG, "channelActive");
        ctx.writeAndFlush("220 welcome to ftp\r\n");
        session = new Session(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg == null) {
            ctx.writeAndFlush("220 welcome to ftp\r\n");
            return;
        }

        String command = msg.toString().trim();
        SLog.v(TAG, "channelRead:" + command);
        int index = command.indexOf(' ');
        String cmd;
        String args = null;
        if (index > 0) {
            cmd = command.substring(0, index);
            if (command.length() > index) {
                args = command.substring(index + 1);
            }
        } else {
            cmd = command;
        }
        session.args = args;
        CommandFactory.handleCommand(session, cmd);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        SLog.i(TAG, "channelWritabilityChanged:" + ctx.channel().isWritable());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SLog.e(TAG, "exceptionCaught: ", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SLog.w(TAG, "channelInactive: ");
        session = null;
    }
}
