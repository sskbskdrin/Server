package cn.sskbskdrin.server.ftp;

import cn.sskbskdrin.log.L;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author ex-keayuan001
 */
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

        String command = msg.toString().trim();
        L.v(TAG, "channelRead:" + command);
        String[] data = command.split(" ");
        CommandFactory.Command commandSolver = CommandFactory.createCommand(data[0]);
        if (commandSolver == null) {
            ctx.writeAndFlush("502 命令未实现\r\n");
        } else {
            if (loginVerify(commandSolver)) {
                String content = "";
                if (data.length >= 2) {
                    content = data[1];
                }
                commandSolver.getResult(content, ctx, this);
            } else {
                ctx.writeAndFlush("532 执行该命令需要登录，请登录后再执行相应的操作\r\n");
            }
        }
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
        USER.remove();
    }

    public final ThreadLocal<String> USER = new ThreadLocal<>();

    private boolean isPortMode;

    /**
     * 数据传输方式 A I
     */
    private String type;
    /**
     * 数据连接的ip
     */
    private String dataIp;
    /**
     * 数据连接的port
     */
    private int dataPort;
    /**
     * 当前目录
     */
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
