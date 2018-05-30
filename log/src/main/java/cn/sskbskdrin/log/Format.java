package cn.sskbskdrin.log;

import cn.sskbskdrin.log.disk.DiskFormat;
import cn.sskbskdrin.log.logcat.PrettyFormat;

/**
 * Used to determine how messages should be printed or saved.
 *
 * @author ex-keayuan001
 * @see PrettyFormat
 * @see DiskFormat
 */
public interface Format {

    String NEW_LINE = System.getProperty("line.separator");

    String formatTag(int priority, String tag);

    String format(String msg);
}
