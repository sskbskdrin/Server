package cn.sskbskdrin.log.logcat;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import cn.sskbskdrin.log.Format;
import cn.sskbskdrin.log.Logger;

/**
 * Draws borders around the given log message along with additional information such as :
 * <p>
 * <ul>
 * <li>Thread information</li>
 * <li>Method stack trace</li>
 * </ul>
 * <p>
 * <pre>
 *  ┌────────Thread information──────────────────
 *  │ Method stack history
 *  ├--------------------------------------------
 *  │ Log message
 *  └────────────────────────────────────────────
 * </pre>
 * <p>
 * <h3>Customize</h3>
 * <pre><code>
 *   Format formatStrategy = PrettyFormat.newBuilder()
 *       .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
 *       .methodCount(0)         // (Optional) How many method line to show. Default 2
 *       .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
 *       .logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
 *       .tag("My custom tag")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
 *       .build();
 * </code></pre>
 */
public class PrettyFormat implements Format {

    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 8;

    /**
     * Drawing toolbox
     */
    private static final char BOTTOM_LEFT_CORNER = '└';
    private static final char HORIZONTAL_LINE = '│';
    private static final String MIDDLE_BORDER = "================================";
    private static final String SINGLE_DIVIDER =
        "│------------------------------------------------------------------------------------------------";
    private static final String THREAD_INFO = "┌" + MIDDLE_BORDER + "    Thread:%s    ID:%s    " + MIDDLE_BORDER;
    private static final String BOTTOM_BORDER = "└" + MIDDLE_BORDER + MIDDLE_BORDER + MIDDLE_BORDER;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String METHOD_INFO = "%s%s.%s (%s:%s)";

    private int methodCount = 10;
    private int methodOffset = 0;
    private boolean logMethod = true;

    private final StringBuilder mBuilder;

    public PrettyFormat() {
        this(6, 0);
    }

    public PrettyFormat(int count, int offset) {
        mBuilder = new StringBuilder(CHUNK_SIZE);
        methodCount = count;
        if (methodCount < 0) {
            methodCount = 0;
        }
        methodOffset = offset;
        if (methodOffset < 0) {
            methodOffset = 0;
        }
    }

    private void logHeaderContent(StringBuilder builder) {
        Thread thread = Thread.currentThread();
        builder.append(String.format(THREAD_INFO, thread.getName(), android.os.Process.myTid()));
    }

    private void logMethod(StringBuilder builder) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        int stackOffset = getStackOffset(trace) + methodOffset;

        StringBuilder level = new StringBuilder(" ");
        for (int i = stackOffset + methodCount; i >= stackOffset; i--) {
            if (i >= trace.length || i < 0) {
                continue;
            }
            builder.append(LINE_SEPARATOR);
            builder.append(HORIZONTAL_LINE);
            builder.append(String.format(METHOD_INFO, level.toString(), trace[i].getClassName(), trace[i]
                .getMethodName(), trace[i].getFileName(), trace[i].getLineNumber()));
            level.append("  ");
        }
        builder.append(LINE_SEPARATOR);
        builder.append(SINGLE_DIVIDER);
    }

    private void logBottomBorder(StringBuilder builder) {
        builder.append(LINE_SEPARATOR);
        builder.append(BOTTOM_BORDER);
    }

    private void logContent(StringBuilder builder, String msg) {
        builder.append(LINE_SEPARATOR);
        builder.append(HORIZONTAL_LINE);
        builder.append(msg);
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(@NonNull StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(Logger.class.getName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String formatTag(int priority, @Nullable String tag) {
        logMethod = priority >= Logger.WARN;
        return tag;
    }

    @Override
    public String format(String msg) {
        mBuilder.setLength(0);
        logHeaderContent(mBuilder);
        if (logMethod) {
            logMethod(mBuilder);
        }
        if (msg.length() <= CHUNK_SIZE) {
            logContent(mBuilder, msg);
        } else {
            byte[] bytes = msg.getBytes();
            for (int i = 0; i < msg.length(); i += CHUNK_SIZE) {
                int count = Math.min(bytes.length - i, CHUNK_SIZE);
                logContent(mBuilder, msg.substring(i, i + count));
            }
        }
        logBottomBorder(mBuilder);
        return mBuilder.toString();
    }

}
