package cn.sskbskdrin.log.disk;

import cn.sskbskdrin.log.Format;
import cn.sskbskdrin.log.Printer;

/**
 * This is used to saves log messages to the disk.
 * By default it uses {@link DiskFormat} to translates text message into CSV format.
 */
public class DiskPrinter implements Printer {

    private Format format;
    private DiskLogStrategy strategy;

    public DiskPrinter() {
        this(null, null);
    }

    public DiskPrinter(DiskLogStrategy strategy, Format formatStrategy) {
        this.strategy = strategy;
        this.format = formatStrategy;

        if (this.strategy == null) {
            this.strategy = new DiskLogStrategy();
        }

        if (this.format == null) {
            this.format = new DiskFormat();
        }
    }

    @Override
    public boolean isLoggable(int priority, String tag) {
        return true;
    }

    @Override
    public void log(int priority, String tag, String message) {
        if (format != null) {
            tag = format.formatTag(priority, tag);
            message = format.format(message);
        }
        strategy.log(priority, tag, tag + ": " + message);
    }
}
