package cn.sskbskdrin.server.ftp;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author sskbskdrin
 * @date 2019-06-18
 */
public class Session {

    private String user;
    /**
     * 用于标记用户是否已经登录
     */
    private boolean isLogin = true;

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

    private ChannelHandlerContext context;

    Session(ChannelHandlerContext ctx) {
        context = ctx;
    }

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

    public boolean isLogin() {
        return isLogin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String args;

    public void writeAndFlush(Object msg) {
        context.writeAndFlush(msg);
    }

    public void write(Object msg) {
        context.write(msg);
    }

    public void flush() {
        context.flush();
    }

    public void close() {
        context.close();
    }

    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) context.channel().remoteAddress();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
