package cn.sskbskdrin.server.ftp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import cn.sskbskdrin.server.util.SLog;

/**
 * @author ex-keayuan001
 */
class CommandFactory {
    private static final String TAG = "CommandFactory";
    public static final String SEPARATOR = File.separator;
    public static final char SEPARATOR_CHAR = File.separatorChar;

    public static void handleCommand(Session session, String type) {
        if (!session.isLogin() && !"USER".equalsIgnoreCase(type) && !"PASS".equalsIgnoreCase(type) && !"QUIT".equalsIgnoreCase(type)) {
            session.writeAndFlush("532 执行该命令需要登录，请登录后再执行相应的操作\r\n");
            return;
        }
        switch (type.toUpperCase()) {
            case "USER":
                user(session);
                break;
            case "PASS":
                pass(session);
                break;
            case "LIST":
                ListCommand(session);
                break;
            case "PWD":
                pwd(session);
                break;
            case "TYPE":
                type(session);
                break;
            case "PORT":
                port(session);
                break;
            case "PASV":
                pasv(session);
                break;
            case "QUIT":
                quit(session);
                break;
            case "RETR":
                RetrCommand(session);
                break;
            case "CWD":
                cwd(session);
                break;
            case "STOR":
                StoreCommand(session);
                break;
            case "SYST":
                stat(session);
                break;
            case "SIZE":
                size(session);
                break;
            case "FEAT":
                feat(session);
                break;
            case "NOOP":
                write(session, 200, "NOOP ok");
                break;
            default:
                write(session, 502, "命令未实现");
        }
    }

    private static void user(Session session) {
        String response;
        if (Share.users.containsKey(session.args) || "anonymous".equals(session.args)) {
            session.setUser(session.args);
            response = "331 用户名正确,需要口令\r\n";
        } else {
            response = "530 用户名错误\r\n";
        }
        session.writeAndFlush(response);
    }

    private static void pass(Session session) {
        String key = session.getUser();
        String pass = Share.users.get(key);

        String response;
        if ("anonymous".equals(key) || pass.equals(session.args)) {
            SLog.i(TAG, "登录成功");
            Share.loginedUser.add(key);
            session.setIsLogin(true);
            response = "230 User " + key + " logged in";
        } else {
            SLog.e(TAG, "登录失败，密码错误");
            response = "530 密码错误";
        }
        session.writeAndFlush(response + "\r\n");
    }

    private static void stat(Session session) {
        write(session, 215, "UNIX Type: L8");
    }

    private static void feat(Session session) {
        write(session, "211-Features:");
        write(session, " UTF-8");
        write(session, "211 End");
    }

    private static void size(Session session) {
        String desDir = Share.rootDir + session.getNowDir();
        String data = session.args;
        if (data == null || "/".equals(data)) {
            desDir = Share.rootDir;
        } else if (data.startsWith("/")) {
            desDir = Share.rootDir + data;
        } else {
            desDir += data;
        }
        File file = new File(desDir);
        if (file.exists() && file.isFile()) {
            session.writeAndFlush("213 " + file.length() + "\r\n");
        } else {
            session.writeAndFlush("550 Could not get file size.\r\n");
        }
    }

    private static void port(Session session) {
        String[] iAp = session.args.split(",");
        String ip = iAp[0] + "." + iAp[1] + "." + iAp[2] + "." + iAp[3];
        int port = 256 * Integer.parseInt(iAp[4]) + Integer.parseInt(iAp[5]);
        SLog.d(TAG, "Port ip=" + ip + ":" + port);
        session.setDataIp(ip);
        session.setDataPort(port);
        session.setPortMode(true);
        session.writeAndFlush("200 the port an ip have been transfer\r\n");
    }

    private static void pasv(Session session) {
        try {
            session.setPortMode(false);
            int port = DataSocketController.start();
            byte[] address = Inet4Address.getLocalHost().getAddress();
            for (byte[] ip : getLocalIPList()) {
                if (ip[0] == 127 && ip[1] == 0 && ip[2] == 0 && ip[3] == 1) {
                } else {
                    address = ip;
                }
            }

            String ip =
                (address[0] & 0xff) + "," + (address[1] & 0xff) + "," + (address[2] & 0xff) + "," + (address[3] & 0xff);
            session.writeAndFlush("227 Entering Passive Mode (" + ip + "," + port / 256 + "," + port % 256 + ")" +
                "\r" + "\n");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static void type(Session session) {
        session.setType(session.args);
        session.writeAndFlush("200 命令正确 转 " + session.args + "模式\r\n");
    }

    private static void pwd(Session session) {
        session.writeAndFlush("257 \"" + session.getNowDir() + "\"\r\n");
    }

    private static void ListCommand(Session session) {
        String desDir = Share.rootDir + session.getNowDir();
        SLog.i(TAG, "List now=" + session.getNowDir() + " args=" + session.args + " des=" + desDir);
        File dir = new File(desDir);
        if (!dir.exists()) {
            session.writeAndFlush("212  文件目录不存在\r\n");
        } else {
            try {
                session.writeAndFlush("150 文件状态正常,ls以 ASCII 方式操作\r\n");
                SLog.i(TAG, "文件目录如下：");

                //开启数据连接，将数据发送给客户端，这里需要有端口号和ip地址
                Socket s;
                if (session.isPortMode()) {
                    s = new Socket(session.getDataIp(), session.getDataPort());
                } else {
                    InetSocketAddress address = session.remoteAddress();
                    s = DataSocketController.getSocket(address.getAddress());
                    while (s == null) {
                        SLog.i(TAG, "wait");
                        Thread.currentThread().join(100);
                        s = DataSocketController.getSocket(address.getAddress());
                    }
                }
                PrintWriter dataWriter = new PrintWriter(s.getOutputStream());
                File[] files = dir.listFiles();
                DateFormat modifyDate = new SimpleDateFormat("MMM dd HH:mm", Locale.US);
                for (File file : files) {
                    String time = modifyDate.format(new Date(file.lastModified()));
                    String content;
                    if (!"-a".equals(session.args) && file.isHidden()) {
                        continue;
                    }
                    String permission;
                    int listSize;
                    long fileLen;
                    if (file.isDirectory()) {
                        permission = "drwxr-xr-x";
                        listSize = file.list().length;
                        fileLen = 0;
                    } else {
                        permission = "-rw-r--r--";
                        listSize = 1;
                        fileLen = file.length();
                    }
                    content = String.format("%s %3d ftp ftp % 12d %s %s", permission, listSize, fileLen, time,
                        file.getName());
                    SLog.i(TAG, "file:" + content);
                    dataWriter.print(content + "\r\n");
                    dataWriter.flush();
                }
                dataWriter.close();
                s.close();
                session.writeAndFlush("226 传输数据连接结束\r\n");
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

    /**
     * 改变工作目录
     */
    private static void cwd(Session session) {
        String newPath = session.getNowDir();
        String data = session.args;
        if (data != null && data.length() > 0) {
            if ("..".equals(data)) {
                int index = session.getNowDir().lastIndexOf(SEPARATOR_CHAR);
                newPath = index > 0 ? session.getNowDir().substring(0, index) : SEPARATOR;
            } else if (data.charAt(0) == SEPARATOR_CHAR) {
                newPath = data;
            } else {
                if (SEPARATOR.equals(session.getNowDir())) {
                    newPath = session.getNowDir() + data;
                } else {
                    newPath = session.getNowDir() + File.separator + data;
                }
            }
        }
        SLog.i(TAG, "cwd curr:" + session.getNowDir() + " new:" + newPath);
        File file = new File(Share.rootDir + newPath);
        if (file.exists()) {
            if (file.isFile()) {
                session.writeAndFlush("550 CWD command successful\r\n");
            } else {
                session.setNowDir(newPath);
                session.writeAndFlush("250 CWD command successful\r\n");
            }
        } else {
            session.writeAndFlush("550 目录不存在");
        }
    }

    /**
     * 处理文件的发送
     */
    private static void RetrCommand(Session session) {
        Socket s;
        String data = session.args;
        String desDir = Share.rootDir + session.getNowDir() + data;
        if (data != null && data.startsWith("/")) {
            desDir = Share.rootDir + data;
        } else {
            if (!session.getNowDir().endsWith("/")) {
                desDir = Share.rootDir + session.getNowDir() + File.separator + data;
            }
        }
        SLog.i(TAG, "Retr file=" + desDir);
        File file = new File(desDir);
        if (file.exists()) {
            try {
                session.write("150 open ascii mode...\r\n");
                session.flush();
                if (session.isPortMode()) {
                    s = new Socket(session.getDataIp(), session.getDataPort());
                } else {
                    InetSocketAddress insocket = (InetSocketAddress) session.remoteAddress();
                    s = DataSocketController.getSocket(insocket.getAddress());
                    while (s == null) {
                        SLog.i(TAG, "wait");
                        Thread.currentThread().join(100);
                        s = DataSocketController.getSocket(insocket.getAddress());
                    }
                }
                byte[] buf = new byte[10 * 1024];
                //                    BufferedOutputStream dataOut = new BufferedOutputStream(s.getOutputStream()
                // , buf.length);
                DataOutputStream dataOut = new DataOutputStream(s.getOutputStream());
                InputStream is = new FileInputStream(file);
                int length;
                while ((length = is.read(buf)) != -1) {
                    if ("A".equals(session.getType())) {
                        dataOut.writeUTF(new String(buf, 0, length));
                    } else {
                        dataOut.write(buf, 0, length);
                    }
                }
                dataOut.flush();
                dataOut.close();
                s.close();
                session.writeAndFlush("226 transfer complete...\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            session.writeAndFlush("553  该文件不存在\r\n");
        }
    }

    private static void StoreCommand(Session session) {
        try {
            session.writeAndFlush("150 Binary args connection\r\n");
            String data = session.args;
            String desDir = Share.rootDir + session.getNowDir() + data;
            if (data != null && data.startsWith("/")) {
                desDir = Share.rootDir + data;
            } else {
                if (!session.getNowDir().endsWith("/")) {
                    desDir = Share.rootDir + session.getNowDir() + File.separator + data;
                }
            }
            SLog.i(TAG, "StoreCommand file=" + desDir);
            File file = new File(desDir);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            //数据连接
            Socket s;
            if (session.isPortMode()) {
                s = new Socket(session.getDataIp(), session.getDataPort());
            } else {
                InetSocketAddress insocket = (InetSocketAddress) session.remoteAddress();
                s = DataSocketController.getSocket(insocket.getAddress());
                while (s == null) {
                    SLog.i(TAG, "wait");
                    Thread.currentThread().join(100);
                    s = DataSocketController.getSocket(insocket.getAddress());
                }
            }
            OutputStream out = new FileOutputStream(file);
            InputStream is = s.getInputStream();
            byte[] byteBuffer = new byte[10 * 1024];
            int length;
            //这里又会阻塞掉，无法从客户端输出流里面获取数据？是因为客户端没有发送数据么
            while ((length = is.read(byteBuffer)) != -1) {
                out.write(byteBuffer, 0, length);
            }
            SLog.i(TAG, "传输完成，关闭连接。。。");
            out.close();
            is.close();
            s.close();
            session.writeAndFlush("226 transfer complete\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void quit(Session session) {
        write(session, 221, "good bye.");
        session.close();
    }

    private static List<byte[]> getLocalIPList() {
        List<byte[]> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        ipList.add(inetAddress.getAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipList;
    }

    private static void write(Session session, String content) {
        if (session != null) {
            session.writeAndFlush(content + "\r\n");
        }
    }

    private static void write(Session session, int code, String content) {
        if (session != null) {
            session.writeAndFlush(code + " " + content + "\r\n");
        }
    }

    public static void main(String[] args) {
        String data[] = new String[3];
        String test = "test";
        for (int i = 0; i < data.length; i++) {
            data[i]="test";
        }
        for (byte[] ip : getLocalIPList()) {
            System.out.println(ip);
        }

        String s1 = "imooc";
        String s2 = "imooc";

        String s5 = "I love ";

        //定义字符串s3，保存“I love”和s1拼接后的内容

        String s3=s5+s1;
        // 比较字符串s1和s2
        // imooc为常量字符串，多次出现时会被编译器优化，只创建一个对象
        System.out.println("s1和s2内存地址相同吗？" + (s1 == s2));

        //比较字符串s1和s3
        System.out.println("s1和s3内存地址相同吗？" + (s1 == s3 ));

        String s4 = s5 + s1;
        //比较字符串s4和s3
        // s1是变量，s4在运行时才知道具体值，所以s3和s4是不同的对象
        System.out.println("s3和s4内存地址相同吗？" + (s4 == s3));
        System.out.println("s3和s4内存地址相同吗？" + (s5 == (s5+"")));
        String s6 = null;
        if(true){
            s6 = "abc";
        }
        s5 = "abc";
        System.out.println("s6 abc?"+(s6=="abc"));

    }

}
