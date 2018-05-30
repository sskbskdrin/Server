package cn.sskbskdrin.log.disk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.sskbskdrin.log.Format;

import static cn.sskbskdrin.log.Logger.ASSERT;
import static cn.sskbskdrin.log.Logger.DEBUG;
import static cn.sskbskdrin.log.Logger.ERROR;
import static cn.sskbskdrin.log.Logger.INFO;
import static cn.sskbskdrin.log.Logger.VERBOSE;
import static cn.sskbskdrin.log.Logger.WARN;

/**
 * CSV formatted file logging for Android.
 * Writes to CSV the following data:
 * epoch timestamp, ISO8601 timestamp (human-readable), log level, tag, log message.
 */
public class DiskFormat implements Format {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String SEPARATOR = " ";

    private final Date date = new Date();
    private final SimpleDateFormat dateFormat;

    public DiskFormat() {
        dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.UK);
    }

    public DiskFormat(SimpleDateFormat format) {
        dateFormat = format;
    }

    @Override
    public String formatTag(int priority, String tag) {
        if (tag == null) {
            tag = "";
        }
        date.setTime(System.currentTimeMillis());
        return dateFormat.format(date) + SEPARATOR + logLevel(priority) + SEPARATOR + tag;
    }

    @Override
    public String format(String msg) {
        return msg + NEW_LINE;
    }

    static String logLevel(int value) {
        switch (value) {
            case VERBOSE:
                return "VERBOSE";
            case DEBUG:
                return "DEBUG";
            case INFO:
                return "INFO";
            case WARN:
                return "WARN";
            case ERROR:
                return "ERROR";
            case ASSERT:
                return "ASSERT";
            default:
                return "UNKNOWN";
        }
    }
}
