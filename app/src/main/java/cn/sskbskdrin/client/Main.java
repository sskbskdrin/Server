package cn.sskbskdrin.client;

import android.os.Environment;

import cn.sskbskdrin.server.ftp.FtpServer;
import cn.sskbskdrin.server.rtmp.RTMPServer;
import cn.sskbskdrin.server.socket.NettyServer;
import cn.sskbskdrin.server.util.SLog;
import cn.sskbskdrin.server.util.SysUtil;

/**
 * @author sskbskdrin
 * @date 2019/April/23
 */
public class Main {
    private static final String TAG = "Main";
    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        SLog.d(TAG, "running");
        if (args != null && args.length > 0) {
            for (String arg : args) {
                String[] name = arg.split(":");
                int port = 0;
                if (name.length > 1) {
                    port = SysUtil.parseInt(name[1]);
                }
                if ("http".equals(name[0])) {
                    if (port == 0) {
                        port = 8080;
                    }
                    //                    HttpServer.getInstance().start(port);
                } else if ("ftp".equals(name[0])) {
                    if (port == 0) {
                        port = 2121;
                    }
                    FtpServer.getInstance().start(Environment.getExternalStorageDirectory().getAbsolutePath(), port);
                } else if ("socket".equals(name[0])) {
                    if (port == 0) {
                        port = 8088;
                    }
                    NettyServer.getInstance().start(port);
                } else if ("rtmp".equals(name[0])) {
                    if (port == 0) {
                        port = 1935;
                    }
                    RTMPServer.getInstance().start(port);
                } else {
                    SLog.e(TAG, "illegal params " + name[0]);
                }
            }
        } else {
            SLog.w(TAG, "Lack of parameters");
        }
        while (true) {
            synchronized (LOCK) {
                try {
                    LOCK.wait();
                    System.out.println("main: end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}