package cn.ssbskdrin.server.files;

import cn.sskbskdrin.log.SSKLog;
import cn.sskbskdrin.log.console.ConsolePrinter;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class Log {
    private static final String TAG = "Log";

    public static SSKLog log = new SSKLog();

    static {
        log.addPinter(new ConsolePrinter());
        log.enableJsonOrXml(false, false);
    }

    public static void v(String msg) {
        log.v(TAG, msg);
    }

    public static void d(String msg) {
        log.d(TAG, msg);
    }

    public static void i(String msg) {
        log.i(TAG, msg);
    }

    public static void w(String msg) {
        log.w(TAG, msg);
    }

    public static void w(String msg, Throwable e) {
        log.w(TAG, msg, e);
    }

    public static void e(String msg) {
        log.e(TAG, msg);
    }

    public static void e(String msg, Throwable e) {
        log.e(TAG, msg, e);
    }

}
