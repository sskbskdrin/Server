package cn.sskbskdrin.server.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class L {

    private static final String DEFAULT_TAG = "DEFAULT";

    private static String logPath = System.getProperty("user.dir");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS ", Locale.US);
    private static Date date = new Date();

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;

    public static int log_Level = 1;
    public static boolean isLogCat = true;
    private static Method mLog;
    private static Class mLogClass;

    public static void v(String msg) {
        println(VERBOSE, DEFAULT_TAG, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void d(String msg) {
        println(DEBUG, DEFAULT_TAG, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void i(String msg) {
        println(INFO, DEFAULT_TAG, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void w(String msg) {
        println(WARN, DEFAULT_TAG, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void e(String msg) {
        println(ERROR, DEFAULT_TAG, msg);
        //        Message.obtain(mHandler, log.hashCode(), log).sendToTarget();
    }

    public static void e(String msg, Throwable e) {
        println(ERROR, DEFAULT_TAG, msg);
        e.printStackTrace();
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
            if (isLogCat) {
                try {
                    if (mLog == null) {
                        mLogClass = Class.forName("android.util.Log");
                        if (mLogClass != null) {
                            mLog = mLogClass.getMethod("println", int.class, String.class, String.class);
                        }
                    }
                    mLog.invoke(mLogClass, level, tag, msg);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                return;
            }
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
                default:
            }
            date.setTime(System.currentTimeMillis());
            String log = start + dateFormat.format(date) + Thread.currentThread().getId() + type + tag + ": " + msg +
                "\u001b[0m";
            System.out.println(log);
        }
    }

}
