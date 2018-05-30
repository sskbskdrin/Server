package cn.sskbskdrin.log.logcat;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import cn.sskbskdrin.log.LogStrategy;

/**
 * LogCat implementation for {@link LogStrategy}
 * <p>
 * This simply prints out all logs to Logcat by using standard {@link Log} class.
 */
public class LogcatStrategy implements LogStrategy {

    @Override
    public void log(int priority, @Nullable String tag, @NonNull String message) {
        Log.println(priority, tag, message);
    }
}
