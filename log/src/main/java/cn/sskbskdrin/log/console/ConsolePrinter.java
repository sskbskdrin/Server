package cn.sskbskdrin.log.console;

import cn.sskbskdrin.log.Printer;

public class ConsolePrinter implements Printer {

    private ConsoleFormat format;

    public ConsolePrinter() {
        format = new ConsoleFormat();
    }

    @Override
    public boolean isLoggable(int priority, String tag) {
        return true;
    }

    @Override
    public void log(int priority, String tag, String message) {
        System.out.println(format.formatTag(priority, tag) + format.format(message));
    }
}
