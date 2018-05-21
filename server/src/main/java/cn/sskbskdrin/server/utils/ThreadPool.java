package cn.sskbskdrin.server.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by ex-keayuan001 on 2018/3/15.
 *
 * @author ex-keayuan001
 */
public class ThreadPool {
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private ExecutorService mExecutorService;

    private static ThreadPool mInstance;

    private ThreadPool() {
        mExecutorService = new ThreadPoolExecutor(5, 5, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, new
                LinkedBlockingQueue<Runnable>());
    }

    public static <T> void execute(final Callback<T> callback) {
        if (mInstance == null) {
            mInstance = new ThreadPool();
        }
        mInstance.mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                final T t = callback.background();
                callback.callback(t);
            }
        });
    }

    public static void run(final Runnable runnable) {
        if (mInstance == null) {
            mInstance = new ThreadPool();
        }
        mInstance.mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                runnable.run();
            }
        });
    }

    public interface Callback<T> {
        T background();

        void callback(T t);
    }
}
