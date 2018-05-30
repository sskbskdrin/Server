package cn.sskbskdrin.server.ftp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cn.sskbskdrin.log.Logger;

public class DataSocketController {
    private static final String TAG = "DataSocketController";
    private static ServerSocket mServerSocket;
    private static List<Socket> mSockets;
    private static int port = 62121;

    public static int start() {
        if (mServerSocket == null) {
            synchronized (DataSocketController.class) {
                if (mServerSocket == null) {
                    Logger.d(TAG, "data socket start");
                    mSockets = new ArrayList<>(10);
                    try {
                        mServerSocket = new ServerSocket(port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Logger.d(TAG, "data socket run");
                            try {
                                while (true) {
                                    Socket s = mServerSocket.accept();
                                    Logger.d(TAG, "data socket accept=" + s
                                            .getRemoteSocketAddress().toString());
                                    synchronized (DataSocketController.class) {
                                        mSockets.add(s);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                Logger.w(TAG, "data socket close");
                                try {
                                    mServerSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mServerSocket = null;
                            }
                        }
                    }).start();
                }
            }
        }
        return port;
    }

    public static Socket getSocket(InetAddress address) {
        Logger.d(TAG, "getSocket address=" + address.toString());
        synchronized (DataSocketController.class) {
            for (int i = 0; i < mSockets.size(); i++) {
                InetAddress temp = mSockets.get(i).getInetAddress();
                if (temp != null && temp.equals(address)) {
                    return mSockets.remove(i);
                }
            }
        }
        return null;
    }
}
