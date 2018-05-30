package cn.sskbskdrin.log;

/**
 * A proxy interface to enable additional operations.
 * Contains all possible Log message usages.
 */
public interface LogHelper {

    void addAdapter(Printer adapter);

    void clearAdapters();

    boolean useGlobalTag();

    LogHelper tag(String tag);

    void log(int priority, String tag, String message, Throwable throwable);
}
