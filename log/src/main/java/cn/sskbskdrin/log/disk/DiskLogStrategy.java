package cn.sskbskdrin.log.disk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import cn.sskbskdrin.log.LogStrategy;

/**
 * Abstract class that takes care of background threading the file log operation on Android.
 * implementing classes are free to directly perform I/O operations there.
 *
 * @author ex-keayuan001
 */
public class DiskLogStrategy extends HandlerThread implements LogStrategy {

    private int mMaxFileSize = 100 * 1024;

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
        Message.obtain(mHandler, level, message).sendToTarget();
    }

    private static class WriteHandler extends Handler {

        private final String folder;
        private final int maxFileSize;

        WriteHandler(Looper looper, String folder, int maxFileSize) {
            super(looper);
            this.folder = folder;
            this.maxFileSize = maxFileSize;
        }

        @Override
        public void handleMessage(Message msg) {
            String content = (String) msg.obj;

            FileWriter fileWriter = null;
            File logFile = getLogFile(folder, "logs");

            try {
                fileWriter = new FileWriter(logFile, true);
                writeLog(fileWriter, content);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                if (fileWriter != null) {
                    try {
                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e1) { /* fail silently */ }
                }
            }
        }

        /**
         * This is always called on a single background thread.
         * Implementing classes must ONLY write to the fileWriter and nothing more.
         * The abstract class takes care of everything else including close the stream and catching IOException
         *
         * @param fileWriter an instance of FileWriter already initialised to the correct file
         */
        private void writeLog(FileWriter fileWriter, String content) throws IOException {
            fileWriter.append(content);
        }

        private File getLogFile(String folderName, String fileName) {
            File folder = new File(folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            int newFileCount = 0;
            File newFile;
            File existingFile = null;

            newFile = new File(folder, String.format("%s_%s.txt", fileName, newFileCount));
            while (newFile.exists()) {
                existingFile = newFile;
                newFileCount++;
                newFile = new File(folder, String.format("%s_%s.txt", fileName, newFileCount));
            }

            if (existingFile != null) {
                if (existingFile.length() >= maxFileSize) {
                    return newFile;
                }
                return existingFile;
            }

            return newFile;
        }
    }
}
