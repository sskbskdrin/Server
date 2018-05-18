package cn.sskbskdrin.server.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ex-keayuan001
 */
public class CommandFactory {
    interface Command {

        /**
         * @param data   从ftp客户端接收的除ftp命令之外的数据
         * @param writer 网络输出流
         * @param t      控制连接所对应的处理线程
         */
        void getResult(String data, Writer writer, ControllerThread t);
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
            default:
                return null;
        }
    }

    public static class UserCommand implements Command {
        /**
         * 检验是否有这个用户名存在
         */
        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            String response = "";
            if (Share.users.containsKey(data)) {
                ControllerThread.USER.set(data);
                response = "331 用户名正确,需要口令\r\n";
            } else {
                response = "530 用户名错误\r\n";
            }
            try {
                writer.write(response);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PassCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            System.out.println("execute the pass command");
            System.out.println("the data is " + data);
            //获得用户名
            String key = ControllerThread.USER.get();
            String pass = Share.users.get(key);

            String response = null;
            if (pass.equals(data)) {
                System.out.println("登录成功");
                Share.loginedUser.add(key);
                t.setIsLogin(true);
                response = "230 User " + key + " logged in";
            } else {
                System.out.println("登录失败，密码错误");
                response = "530   密码错误";
            }
            try {
                writer.write(response);
                writer.write("\r\n");
                writer.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static class SystCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            System.out.println("the data is " + data);
            try {
                writer.write("215 UNIX Type: L8  \r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PortCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            String response = "200 the port an ip have been transfered";
            try {
                String[] iAp = data.split(",");
                String ip = iAp[0] + "." + iAp[1] + "." + iAp[2] + "." + iAp[3];
                String port = Integer.toString(256 * Integer.parseInt(iAp[4]) + Integer.parseInt(iAp[5]));
                System.out.println("ip is " + ip);
                System.out.println("port is " + port);
                t.setDataIp(ip);
                t.setDataPort(port);
                writer.write(response);
                writer.write("\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TypeCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            try {
                writer.write("200 命令正确 转 BINARY 模式\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PwdCommand implements Command {

        /**
         * 获取ftp目录里面的文件列表
         */
        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            System.out.println("pwd data=" + data);
            try {
                writer.write("257 \"" + t.getNowDir() + "\" transfer complete...\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ListCommand implements Command {
        /**
         * 获取ftp目录里面的文件列表
         */
        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            String desDir = Share.rootDir + t.getNowDir() + data;
            System.out.println("now:" + t.getNowDir());
            System.out.println("data:" + data);
            System.out.println("des:" + desDir);
            File dir = new File(desDir);
            if (!dir.exists()) {
                try {
                    writer.write("210  文件目录不存在\r\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    writer.write("150 文件状态正常,ls以 ASCII 方式操作");
                    StringBuilder dirs = new StringBuilder();
                    System.out.println("文件目录如下：");
                    dirs.append("文件目录如下:\n");
                    String[] lists = dir.list();
                    String flag = null;

                    String fileType;
                    //开启数据连接，将数据发送给客户端，这里需要有端口号和ip地址
                    Socket s;
                    writer.write("150 open ascii mode...\r\n");
                    writer.flush();
                    s = new Socket(t.getDataIp(), Integer.parseInt(t.getDataPort()));
                    PrintWriter dataWriter = new PrintWriter(s.getOutputStream());
                    File[] files = dir.listFiles();
                    String modifyDate;
                    for (int i = 0; i < files.length; i++) {
                        modifyDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(files[i].lastModified()));
                        //                        modifyDate = "" + files[i].lastModified();
                        String content;
                        if (files[i].isDirectory()) {
                            content = "drwxr-xr-x 1 ftp ftp 0 " + modifyDate + " " + files[i].getName();
                        } else {
                            content = "-rw-r--r-- 1 ftp ftp " + files[i].length() + " " + modifyDate + " " + files[i]
                                .getName();
                        }
                        System.out.println("file:" + content);
                        dataWriter.println(content);
                        dataWriter.flush();
                    }
                    dataWriter.println("total:" + files.length);
                    dataWriter.flush();
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
                }
            }
        }
    }

    /**
     * 改变工作目录
     */
    public static class CwdCommand implements Command {
        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            System.out.println("cwd:" + data);
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
            System.out.println("cwd dir:" + dir);
            System.out.println("cwd now:" + t.getNowDir());
            File file = new File(dir);
            try {
                if (file.exists() && file.isDirectory()) {
                    writer.write("250 CWD command succesful");
                } else {
                    writer.write("550 目录不存在");
                }
                writer.write("\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理文件的发送
     */
    public static class RetrCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            Socket s;
            String desDir = t.getNowDir() + File.separator + data;
            File file = new File(desDir);
            System.out.println(desDir);
            if (file.exists()) {
                try {
                    writer.write("150 open ascii mode...\r\n");
                    writer.flush();
                    s = new Socket(t.getDataIp(), Integer.parseInt(t.getDataPort()));
                    BufferedOutputStream dataOut = new BufferedOutputStream(s.getOutputStream());
                    byte[] buf = new byte[1024];
                    InputStream is = new FileInputStream(file);
                    while (-1 != is.read(buf)) {
                        dataOut.write(buf);
                    }
                    dataOut.flush();
                    s.close();
                    writer.write("220 transfer complete...\r\n");
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    writer.write("220  该文件不存在\r\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static class StoreCommand implements Command {

        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {
            try {
                writer.write("150 Binary data connection\r\n");
                writer.flush();
                RandomAccessFile inFile = new RandomAccessFile(t.getNowDir() + "/" + data, "rw");
                //数据连接
                Socket tempSocket = new Socket(t.getDataIp(), Integer.parseInt(t.getDataPort()));
                InputStream inSocket = tempSocket.getInputStream();
                byte byteBuffer[] = new byte[1024];
                int amount;
                //这里又会阻塞掉，无法从客户端输出流里面获取数据？是因为客户端没有发送数据么
                while ((amount = inSocket.read(byteBuffer)) != -1) {
                    inFile.write(byteBuffer, 0, amount);
                }
                System.out.println("传输完成，关闭连接。。。");
                inFile.close();
                inSocket.close();
                tempSocket.close();
                //断开数据连接

                writer.write("226 transfer complete\r\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class QuitCommand implements Command {
        @Override
        public void getResult(String data, Writer writer, ControllerThread t) {

            try {
                writer.write("221 goodbye.\r\n");
                writer.flush();
                writer.close();
                t.getSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
