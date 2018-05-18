package cn.sskbskdrin.server.ftp;

/**
 * Created by ex-keayuan001 on 2018/5/17.
 *
 * @author ex-keayuan001
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import cn.sskbskdrin.server.ftp.CommandFactory.Command;

/**
 * @author onroadrui
 * 用于处理控制连接数据请求的线程
 * 控制连接:在创建之后，直到socket.close()(四次挥手的过程)，
 * 都是tcp里面的establish的阶段。可以自由地传输数据（全双工的）
 */
public class ControllerThread extends Thread {

    private int count = 0;

    //客户端socket与服务器端socket组成一个tcp连接
    private Socket socket;

    //当前的线程所对应的用户
    public static final ThreadLocal<String> USER = new ThreadLocal<String>();

    //数据连接的ip
    private String dataIp;

    //数据连接的port
    private String dataPort;

    //用于标记用户是否已经登录
    private boolean isLogin = false;

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

    public boolean getIsLogin() {
        return isLogin;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getDataIp() {
        return dataIp;
    }

    public void setDataIp(String dataIp) {
        this.dataIp = dataIp;
    }

    public String getDataPort() {
        return dataPort;
    }

    public void setDataPort(String dataPort) {
        this.dataPort = dataPort;
    }

    public ControllerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("hello");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            while (true) {
                //第一次访问，输入流里面是没有东西的，所以会阻塞住
                if (count == 0) {
                    writer.println("220 准备为您服务\r\n");
                    writer.flush();
                    count++;
                } else {
                    //两种情况会关闭连接：(1)quit命令 (2)密码错误
                    if (!socket.isClosed()) {
                        //进行命令的选择，然后进行处理，当客户端没有发送数据的时候，将会阻塞
                        String command = reader.readLine();
                        if (command != null) {
                            System.out.println("command=" + command);
                            String[] datas = command.split(" ");
                            CommandFactory.Command commandSolver = CommandFactory.createCommand(datas[0]);
                            //登录验证,在没有登录的情况下，无法使用除了user,pass之外的命令
                            if (loginValiate(commandSolver)) {
                                if (commandSolver == null) {
                                    writer.println("502  该命令不存在，请重新输入\r\n");
                                    writer.flush();
                                } else {
                                    String data = "";
                                    if (datas.length >= 2) {
                                        data = datas[1];
                                    }
                                    commandSolver.getResult(data, writer, this);
                                }
                            } else {
                                writer.println("532 执行该命令需要登录，请登录后再执行相应的操作\r\n");
                                writer.flush();
                            }
                        }
                    } else {
                        //连接已经关闭，这个线程不再有存在的必要
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("结束tcp连接");
        }
    }

    public boolean loginValiate(Command command) {
        if (command instanceof CommandFactory.UserCommand || command instanceof CommandFactory.PassCommand) {
            return true;
        } else {
            return isLogin;
        }
    }
}
