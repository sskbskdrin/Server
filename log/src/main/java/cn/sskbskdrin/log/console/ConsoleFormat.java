package cn.sskbskdrin.log.console;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.sskbskdrin.log.Format;
import cn.sskbskdrin.log.Logger;

/**
 * CSV formatted file logging for Android.
 * Writes to CSV the following data:
 * epoch timestamp, ISO8601 timestamp (human-readable), log level, tag, log message.
 */
public class ConsoleFormat implements Format {

    private final Date date = new Date();
    private final SimpleDateFormat dateFormat;

    public ConsoleFormat() {
        dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.UK);
    }

    public ConsoleFormat(SimpleDateFormat format) {
        dateFormat = format;
    }

    @Override
    public String formatTag(int priority, String tag) {
        if (tag == null) {
            tag = "";
        }
        String type = "";
        String start = "";
        switch (priority) {
            case Logger.VERBOSE:
                type = " V/";
                start = "\u001b[30;37m";
                break;
            case Logger.DEBUG:
                type = " D/";
                start = "\u001b[30;34m";
                break;
            case Logger.INFO:
                type = " I/";
                start = "\u001b[30;32m";
                break;
            case Logger.WARN:
                type = " W/";
                start = "\u001b[30;33m";
                break;
            case Logger.ERROR:
                type = " E/";
                start = "\u001b[30;31m";
                break;
            default:
        }
        date.setTime(System.currentTimeMillis());
        return start + dateFormat.format(date) + Thread.currentThread().getId() + type + tag + ": ";
    }

    @Override
    public String format(String msg) {
        return msg + "\u001b[0m";
    }
}
