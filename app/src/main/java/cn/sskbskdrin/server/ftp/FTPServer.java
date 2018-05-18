package cn.sskbskdrin.server.ftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ex-keayuan001 on 2018/5/17.
 *
 * @author ex-keayuan001
 */
public class FTPServer {

    private int port;

    ServerSocket serverSocket;

    public FTPServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        //初始化系统信息
        Share.init();
    }

    public void listen() throws IOException {
        Socket socket = null;
        while (true) {
            //这个是建立连接,三次握手的过程，当连接建立了之后，两个socket之间的通讯是直接通过流进行的，不用再通过这一步
            socket = serverSocket.accept();
            ControllerThread thread = new ControllerThread(socket);
            thread.start();
        }
    }

    public static void main(String args[]) throws IOException {
        FTPServer ftpServer = new FTPServer(2100);
        ftpServer.listen();
    }
}
