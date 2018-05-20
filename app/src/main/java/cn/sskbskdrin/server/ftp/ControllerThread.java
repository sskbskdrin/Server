package cn.sskbskdrin.server.ftp;

/**
 * Created by ex-keayuan001 on 2018/5/17.
 *
 * @author ex-keayuan001
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import cn.sskbskdrin.server.ftp.CommandFactory.Command;
import cn.sskbskdrin.server.utils.L;

/**
 * @author onroadrui
 * 用于处理控制连接数据请求的线程
 * 控制连接:在创建之后，直到socket.close()(四次挥手的过程)，
 * 都是tcp里面的establish的阶段。可以自由地传输数据（全双工的）
 */
public class ControllerThread implements Runnable {
    private static final String TAG = "ControllerThread";

    private int count = 0;

    //客户端socket与服务器端socket组成一个tcp连接
    private Socket socket;

    //当前的线程所对应的用户
    public static final ThreadLocal<String> USER = new ThreadLocal<>();

    private boolean isPortMode;

    //数据连接的ip
    private String dataIp;
    //数据连接的port
    private int dataPort;

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

    public Socket getSocket() {
        return socket;
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

    public ControllerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(socket.getInetAddress().toString());
        L.d(TAG, "ControllerThread name=" + socket.getInetAddress().toString());
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            //第一次访问，输入流里面是没有东西的，所以会阻塞住
            writer.println("220 准备为您服务\n\r");
            writer.flush();

            //两种情况会关闭连接：(1)quit命令 (2)密码错误
            while (!socket.isClosed()) {
                //进行命令的选择，然后进行处理，当客户端没有发送数据的时候，将会阻塞
                String command = reader.readLine();
                if (command != null) {
                    L.i(TAG, "command=" + command);
                    String[] datas = command.split(" ");
                    CommandFactory.Command commandSolver = CommandFactory.createCommand(datas[0]);
                    if (commandSolver == null) {
                        writer.println("502 命令未实现\n\r");
                        writer.flush();
                    } else {
                        //登录验证,在没有登录的情况下，无法使用除了user,pass之外的命令
                        if (loginVerify(commandSolver)) {
                            String data = "";
                            if (datas.length >= 2) {
                                data = datas[1];
                            }
                            //                            commandSolver.getResult(data, writer, this);
                        } else {
                            writer.println("532 执行该命令需要登录，请登录后再执行相应的操作\n\r");
                            writer.flush();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            L.e(TAG, "结束tcp连接");
        }
    }

    private boolean loginVerify(Command command) {
        return command instanceof CommandFactory.UserCommand || command instanceof CommandFactory.PassCommand ||
                command instanceof CommandFactory.QuitCommand || isLogin;
    }

    public boolean isPortMode() {
        return isPortMode;
    }

    public void setPortMode(boolean portMode) {
        isPortMode = portMode;
    }
}
