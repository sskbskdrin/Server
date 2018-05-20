package cn.sskbskdrin.server.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.sskbskdrin.server.utils.L;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author ex-keayuan001
 */
public class CommandFactory {
    private static final String TAG = "CommandFactory";

    interface Command {

        /**
         * @param data   从ftp客户端接收的除ftp命令之外的数据
         * @param writer 网络输出流
         * @param t      控制连接所对应的处理线程
         */
        void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t);
    }

    public static Command createCommand(String type) {

        type = type.toUpperCase();
        switch (type) {
            case "USER":
                return new UserCommand();
            case "PASS":
                return new PassCommand();
            case "LIST":
                return new ListCommand();
            case "PWD":
                return new PwdCommand();
            case "TYPE":
                return new TypeCommand();
            case "PORT":
                return new PortCommand();
            case "PASV":
                return new PasvCommand();
            case "QUIT":
                return new QuitCommand();
            case "RETR":
                return new RetrCommand();
            case "CWD":
                return new CwdCommand();
            case "STOR":
                return new StoreCommand();
            case "SYST":
                return new SystCommand();
            case "SIZE":
                return new SizeCommand();
            default:
                return null;
        }
    }

    public static class UserCommand implements Command {
        /**
         * 检验是否有这个用户名存在
         */
        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            String response;
            if (Share.users.containsKey(data)) {
                t.USER.set(data);
                response = "331 用户名正确,需要口令\r\n";
            } else {
                response = "530 用户名错误\r\n";
            }
            writer.writeAndFlush(response);
        }
    }

    public static class PassCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            L.i(TAG, "execute the pass command");
            L.i(TAG, "the data is " + data);
            //获得用户名
            String key = t.USER.get();
            String pass = Share.users.get(key);

            String response;
            if (pass.equals(data)) {
                L.i(TAG, "登录成功");
                Share.loginedUser.add(key);
                t.setIsLogin(true);
                response = "230 User " + key + " logged in";
            } else {
                L.e(TAG, "登录失败，密码错误");
                response = "530   密码错误";
            }
            writer.writeAndFlush(response + "\r\n");
        }
    }

    public static class SystCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            L.i(TAG, "the data is " + data);
            writer.write("215 UNIX Type: L8  \r\n");
            writer.flush();
        }
    }

    public static class SizeCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            L.i(TAG, "size the data is " + data);
            String desDir = Share.rootDir + t.getNowDir();
            if (data == null || "/".equals(data)) {
                desDir = Share.rootDir;
            } else if (data.startsWith("/")) {
                desDir = Share.rootDir + data;
            } else {
                desDir += data;
            }
            File file = new File(desDir);
            if (file.exists()) {
                writer.write("213 " + file.length() + "\r\n");
            } else {
                writer.write("213 0\r\n");
            }
            writer.flush();
        }
    }

    public static class PortCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            String response = "200 the port an ip have been transfer";
            String[] iAp = data.split(",");
            String ip = iAp[0] + "." + iAp[1] + "." + iAp[2] + "." + iAp[3];
            int port = 256 * Integer.parseInt(iAp[4]) + Integer.parseInt(iAp[5]);
            L.i(TAG, "ip is " + ip);
            L.i(TAG, "port is " + port);
            t.setDataIp(ip);
            t.setDataPort(port);
            t.setPortMode(true);
            writer.writeAndFlush(response + "\r\n");
        }
    }


    public static class PasvCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            try {
                int port = DataSocketController.start();
                byte[] address = Inet4Address.getLocalHost().getAddress();

                String ip = (address[0] & 0xff) + "," + (address[1] & 0xff) + "," + (address[2] & 0xff) + "," +
                        (address[3] & 0xff);
                writer.writeAndFlush("227 Entering Passive Mode (" + ip + "," + port / 256 + "," + port % 256 + ")" +
                        "\r\n");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TypeCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            writer.writeAndFlush("200 命令正确 转 BINARY 模式\r\n");
        }
    }

    public static class PwdCommand implements Command {

        /**
         * 获取ftp目录里面的文件列表
         */
        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            L.i(TAG, "pwd data=" + data);
            writer.writeAndFlush("257 \"" + t.getNowDir() + "\" transfer complete...\r\n");
            writer.flush();
        }
    }

    public static class ListCommand implements Command {
        /**
         * 获取ftp目录里面的文件列表
         */
        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            String desDir = Share.rootDir + t.getNowDir();
            L.i(TAG, "now:" + t.getNowDir());
            L.i(TAG, "data:" + data);
            L.i(TAG, "des:" + desDir);
            File dir = new File(desDir);
            if (!dir.exists()) {
                writer.writeAndFlush("212  文件目录不存在\r\n");
                writer.flush();
            } else {
                try {
                    writer.write("150 文件状态正常,ls以 ASCII 方式操作\r\n");
                    writer.flush();
                    L.i(TAG, "文件目录如下：");

                    //开启数据连接，将数据发送给客户端，这里需要有端口号和ip地址
                    Socket s;
                    if (t.isPortMode()) {
                        s = new Socket(t.getDataIp(), t.getDataPort());
                    } else {
                        InetSocketAddress insocket = (InetSocketAddress) writer.channel().remoteAddress();
                        s = DataSocketController.getSocket(insocket.getAddress());
                        while (s == null) {
                            L.i(TAG, "wait");
                            Thread.currentThread().join(100);
                            s = DataSocketController.getSocket(insocket.getAddress());
                        }
                    }
                    PrintWriter dataWriter = new PrintWriter(s.getOutputStream());
                    File[] files = dir.listFiles();
                    DateFormat modifyDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    for (File file : files) {
                        String time = modifyDate.format(new Date(file.lastModified()));
                        String content;
                        if (!"-a".equals(data) && file.isHidden()) {
                            continue;
                        }
                        if (file.isDirectory()) {
                            content = "drwxr-xr-x " + file.list().length + " ftp ftp 0 " + time + " " + file.getName();
                        } else {
                            content = "-rw-r--r-- 1 ftp ftp " + file.length() + " " + time + " " + file.getName();
                        }
                        L.i(TAG, "file:" + content);
                        dataWriter.println(content);
                        dataWriter.flush();
                    }
                    dataWriter.close();
                    s.close();
                    writer.write("226 传输数据连接结束\r\n");
                    writer.flush();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 改变工作目录
     */
    public static class CwdCommand implements Command {
        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            L.i(TAG, "cwd:" + data);
            String dir = Share.rootDir;
            if (data != null && data.length() > 0) {
                if ("..".equals(data)) {
                    int index = t.getNowDir().lastIndexOf('/');
                    if (index > 0) {
                        t.setNowDir(t.getNowDir().substring(0, index));
                        dir = Share.rootDir + t.getNowDir();
                    } else {
                        t.setNowDir("/");
                        dir = Share.rootDir;
                    }
                } else if (data.charAt(0) == '/') {
                    t.setNowDir(data);
                    dir = Share.rootDir + data;
                } else {
                    if ("/".equals(t.getNowDir())) {
                        t.setNowDir(t.getNowDir() + data);
                    } else {
                        t.setNowDir(t.getNowDir() + File.separator + data);
                    }
                    dir = Share.rootDir + t.getNowDir();
                }
            }
            L.i(TAG, "cwd dir:" + dir);
            L.i(TAG, "cwd now:" + t.getNowDir());
            File file = new File(dir);
            if (file.exists() && file.isDirectory()) {
                writer.writeAndFlush("250 CWD command successful\r\n");
            } else {
                writer.writeAndFlush("550 目录不存在");
            }
        }
    }

    /**
     * 处理文件的发送
     */
    public static class RetrCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            Socket s;
            String desDir = Share.rootDir + t.getNowDir() + data;
            if (data != null && data.startsWith("/")) {
                desDir = Share.rootDir + data;
            } else {
                if (!t.getNowDir().endsWith("/")) {
                    desDir = Share.rootDir + t.getNowDir() + File.separator + data;
                }
            }
            L.i(TAG, "RetrCommand file=" + desDir);
            File file = new File(desDir);
            if (file.exists()) {
                try {
                    writer.write("150 open ascii mode...\r\n");
                    writer.flush();
                    if (t.isPortMode()) {
                        s = new Socket(t.getDataIp(), t.getDataPort());
                    } else {
                        InetSocketAddress insocket = (InetSocketAddress) writer.channel().remoteAddress();
                        s = DataSocketController.getSocket(insocket.getAddress());
                        while (s == null) {
                            L.i(TAG, "wait");
                            Thread.currentThread().join(100);
                            s = DataSocketController.getSocket(insocket.getAddress());
                        }
                    }
                    BufferedOutputStream dataOut = new BufferedOutputStream(s.getOutputStream());
                    byte[] buf = new byte[10 * 1024];
                    InputStream is = new FileInputStream(file);
                    int length;
                    while ((length = is.read(buf)) != -1) {
                        dataOut.write(buf, 0, length);
                    }
                    dataOut.flush();
                    dataOut.close();
                    s.close();
                    writer.write("226 transfer complete...\r\n");
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                writer.writeAndFlush("553  该文件不存在\r\n");
            }
        }
    }

    public static class StoreCommand implements Command {

        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            try {
                writer.write("150 Binary data connection\r\n");
                writer.flush();
                String desDir = Share.rootDir + t.getNowDir() + data;
                if (data != null && data.startsWith("/")) {
                    desDir = Share.rootDir + data;
                } else {
                    if (!t.getNowDir().endsWith("/")) {
                        desDir = Share.rootDir + t.getNowDir() + File.separator + data;
                    }
                }
                L.i(TAG, "StoreCommand file=" + desDir);
                File file = new File(desDir);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                //数据连接
                Socket s;
                if (t.isPortMode()) {
                    s = new Socket(t.getDataIp(), t.getDataPort());
                } else {
                    InetSocketAddress insocket = (InetSocketAddress) writer.channel().remoteAddress();
                    s = DataSocketController.getSocket(insocket.getAddress());
                    while (s == null) {
                        L.i(TAG, "wait");
                        Thread.currentThread().join(100);
                        s = DataSocketController.getSocket(insocket.getAddress());
                    }
                }
                OutputStream out = new FileOutputStream(file);
                InputStream is = s.getInputStream();
                byte byteBuffer[] = new byte[10 * 1024];
                int length;
                //这里又会阻塞掉，无法从客户端输出流里面获取数据？是因为客户端没有发送数据么
                while ((length = is.read(byteBuffer)) != -1) {
                    out.write(byteBuffer, 0, length);
                }
                L.i(TAG, "传输完成，关闭连接。。。");
                out.close();
                is.close();
                s.close();
                //断开数据连接
                writer.write("226 transfer complete\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class QuitCommand implements Command {
        @Override
        public void getResult(String data, ChannelHandlerContext writer, FtpClientChannel t) {
            writer.writeAndFlush("221 goodbye.\r\n");
            writer.close();
        }
    }
}
