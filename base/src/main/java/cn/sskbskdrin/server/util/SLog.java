package cn.sskbskdrin.server.util;

import cn.sskbskdrin.log.L;

/**
 * @author sskbskdrin
 * @date 2019/April/26
 */
public class SLog {
    public static void append(String msg) {
        L.append(msg);
    }

    public static void v(String tag, String msg) {
        L.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        L.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        L.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        L.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        L.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        L.e(tag, msg, e);
    }

}
