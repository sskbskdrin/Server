package cn.sskbskdrin.server;

import android.os.Looper;

import cn.sskbskdrin.server.http.HttpServer;
import cn.sskbskdrin.server.socket.NettyServer;

/**
 * Created by sskbskdrin on 2018/一月/27.
 */

public class Main {
    private static final String TAG = "Main";

    public static void main(String[] args) {
        System.out.println("running");
        Looper.prepare();
        if (args != null && args.length > 0) {
            if ("http".equals(args[0])) {
                int port = 8080;
                if (args.length > 1) {
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                HttpServer.getInstance().start(port);
            } else if ("socket".equals(args[0])) {
                int port = 8088;
                if (args.length > 1) {
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                NettyServer.start(port);
            } else {
                System.out.println("illegal option");
            }
        } else {
            HttpServer.getInstance().start(8080);
            NettyServer.start(8088);
        }
        Looper.loop();
        System.out.println("main: end");
    }

}
