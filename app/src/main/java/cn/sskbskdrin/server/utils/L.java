package cn.sskbskdrin.server.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class L {

    private static final String TAG = "LogUtil";

    private static String logPath = System.getProperty("user.dir");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS ", Locale.US);
    private static Date date = new Date();

    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;

    public static int log_Level = 1;

    /**
     * 初始化，须在使用之前设置，最好在Application创建时调用
     */
    public static void init() {

    }

    public static void v(String tag, String msg) {
        println(VERBOSE, tag, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void d(String tag, String msg) {
        println(DEBUG, tag, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void i(String tag, String msg) {
        println(INFO, tag, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void w(String tag, String msg) {
        println(WARN, tag, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void e(String tag, String msg) {
        println(ERROR, tag, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void e(String tag, String msg, Throwable e) {
        println(ERROR, tag, msg);
        e.printStackTrace();
    }

    private static void println(int level, String tag, String msg) {
        if (level >= log_Level) {
            String type = "";
            String start = "";
            switch (level) {
                case VERBOSE:
                    type = " V/";
                    start = "\u001b[30;37m";
                    break;
                case DEBUG:
                    type = " D/";
                    start = "\u001b[30;34m";
                    break;
                case INFO:
                    type = " I/";
                    start = "\u001b[30;32m";
                    break;
                case WARN:
                    type = " W/";
                    start = "\u001b[30;33m";
                    break;
                case ERROR:
                    type = " E/";
                    start = "\u001b[30;31m";
                    break;
            }
            date.setTime(System.currentTimeMillis());
            String log = start + dateFormat.format(date) + Thread.currentThread().getId() + type + tag + ": " + msg +
                    "\u001b[0m";
            System.out.println(log);
        }
    }

}
