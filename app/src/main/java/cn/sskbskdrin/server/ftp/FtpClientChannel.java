package cn.sskbskdrin.server.ftp;

import cn.sskbskdrin.server.utils.L;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class FtpClientChannel extends ChannelInboundHandlerAdapter {
    private static final String TAG = "FtpClientChannel";

    /**
     * 用于标记用户是否已经登录
     */
    private boolean isLogin = false;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        L.d(TAG, "channelActive");
        ctx.writeAndFlush("220 welcome to ftp\r\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg == null) {
            ctx.writeAndFlush("220 welcome to ftp\r\n");
            return;
        }
        if (msg instanceof DefaultHttpRequest) {
            ctx.writeAndFlush("220 welcome to ftp\r\n");
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            DefaultHttpResponse response = null;
            ByteBuf content = Unpooled.wrappedBuffer("220 welcome to ftp\r\n".getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            heads.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
            return;
        }
        String command = msg.toString().trim();
        L.d(TAG, "channelRead:" + command);
        String[] datas = command.split(" ");
        CommandFactory.Command commandSolver = CommandFactory.createCommand(datas[0]);
        if (commandSolver == null) {
            ctx.writeAndFlush("502 命令未实现\r\n");
        } else {
            if (loginVerify(commandSolver)) {
                String data = "";
                if (datas.length >= 2) {
                    data = datas[1];
                }
                commandSolver.getResult(data, ctx, this);
            } else {
                ctx.writeAndFlush("532 执行该命令需要登录，请登录后再执行相应的操作\r\n");
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        L.d(TAG, "channelReadComplete:");
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        L.i(TAG, "channelWritabilityChanged:" + ctx.channel().isWritable());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        L.e(TAG, "exceptionCaught: ", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        L.w(TAG, "channelInactive: ");
    }


    //当前的线程所对应的用户
    public final ThreadLocal<String> USER = new ThreadLocal<>();

    private boolean isPortMode;

    //数据连接的ip
    private String dataIp;
    //数据连接的port
    private int dataPort;

    //当前目录
    private String nowDir = "/";

    public String getNowDir() {
        return nowDir;
    }

    public void setNowDir(String nowDir) {
        this.nowDir = nowDir;
    }

    public void setIsLogin(boolean t) {
        isLogin = t;
    }

    public String getDataIp() {
        return dataIp;
    }

    public void setDataIp(String dataIp) {
        this.dataIp = dataIp;
    }

    public int getDataPort() {
        return dataPort;
    }

    public void setDataPort(int dataPort) {
        this.dataPort = dataPort;
    }

    public boolean isPortMode() {
        return isPortMode;
    }

    public void setPortMode(boolean portMode) {
        isPortMode = portMode;
    }

    private boolean loginVerify(CommandFactory.Command command) {
        return command instanceof CommandFactory.UserCommand || command instanceof CommandFactory.PassCommand ||
                command instanceof CommandFactory.QuitCommand || isLogin;
    }

}
