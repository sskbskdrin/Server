package cn.sskbskdrin.log.disk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.sskbskdrin.log.LogStrategy;

/**
 * Abstract class that takes care of background threading the file log operation on Android.
 * implementing classes are free to directly perform I/O operations there.
 *
 * @author ex-keayuan001
 */
public class DiskLogStrategy extends HandlerThread implements LogStrategy {

    private static final int WHAT_CLOSE_FILE = 1001;

    private int mMaxFileSize = 1024 * 1024;

    private String mPath;
    private Handler mHandler;

    public DiskLogStrategy() {
        super("DiskLog", Process.THREAD_PRIORITY_BACKGROUND);
        start();
    }

    public DiskLogStrategy(String path, int maxFile) {
        super("DiskLog", Process.THREAD_PRIORITY_BACKGROUND);
        mPath = path;
        if (maxFile > 1024) {
            mMaxFileSize = maxFile;
        }
        start();
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new WriteHandler(Looper.myLooper(), mPath, mMaxFileSize);
    }

    @Override
    public void log(int level, String tag, String message) {
        mHandler.removeMessages(WHAT_CLOSE_FILE);
        Message.obtain(mHandler, level, message).sendToTarget();
        mHandler.sendEmptyMessageDelayed(WHAT_CLOSE_FILE, 1000);
    }

    private static class WriteHandler extends Handler {

        private final String folder;
        private final int maxFileSize;
        private FileOutputStream out;
        private long length = 0;

        WriteHandler(Looper looper, String folder, int maxFileSize) {
            super(looper);
            this.folder = folder;
            this.maxFileSize = maxFileSize;
        }

        @Override
        public void handleMessage(Message msg) {
            String content = (String) msg.obj;

            if (WHAT_CLOSE_FILE == msg.what) {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    out = null;
                }
                return;
            }

            try {
                if (out == null) {
                    out = getFileOutputStream(folder, "log");
                }
                length += content.length();
                out.write(content.getBytes());
                out.flush();
                if (length > maxFileSize) {
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
            }
        }

        private FileOutputStream getFileOutputStream(String folderName, String fileName) throws FileNotFoundException {
            File folder = new File(folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File newFile = new File(folder, fileName + ".log");
            if (newFile.exists()) {
                length = newFile.length();
                if (length > maxFileSize) {
                    File temp;
                    int newFileCount = 0;
                    do {
                        temp = new File(folder, String.format("%s_%s.log", fileName, newFileCount++));
                    } while (temp.exists());
                    newFile.renameTo(temp);
                    newFile = new File(folder, fileName + ".log");
                }
            }

            return new FileOutputStream(newFile, true);
        }
    }
}
