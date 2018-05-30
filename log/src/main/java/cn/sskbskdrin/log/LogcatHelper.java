package cn.sskbskdrin.log;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * log日志统计保存
 * <p>
 * Created by ex-keayuan001 on 2018/5/14.
 *
 * @author ex-keayuan001
 */
public class LogcatHelper {

    private static final int MAX_LENGTH = 10 * 1024 * 1024;
    private static LogcatHelper INSTANCE = null;
    private static String PATH_LOGCAT;
    private LogDumper mLogDumper = null;
    private int mPId;

    /**
     * 初始化目录
     */
    public void init(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File
                    .separator + "baby";
        } else {
            PATH_LOGCAT = context.getFilesDir().getAbsolutePath() + File.separator + "baby";
        }
        File file = new File(PATH_LOGCAT);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static LogcatHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogcatHelper();
        }
        return INSTANCE;
    }

    private LogcatHelper() {
        mPId = android.os.Process.myPid();
    }

    public void start() {
        if (mLogDumper == null) {
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
        }
        mLogDumper.start();
    }

    public boolean isRunning() {
        if (mLogDumper != null) {
            return mLogDumper.mRunning;
        }
        return false;
    }

    public void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
    }

    private class LogDumper extends Thread {

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        private String cmd;
        private String mPID;
        private String mDir;
        private int length = 0;

        public LogDumper(String pid, String dir) {
            mPID = pid;
            mDir = dir;

            // 日志等级：*:v , *:d , *:w , *:e , *:f , *:s

            // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
            // cmds = "logcat  | grep \"(" + mPID + ")\"";//打印所有日志信息
            // cmds = "logcat -s way";//打印标签过滤信息
            //            cmd = "logcat *:d | grep \"(" + mPID + ")\"";
            cmd = "logcat | grep " + mPID + ")";
        }

        public void stopLogs() {
            mRunning = false;
        }

        private FileOutputStream getOut() {
            try {
                String fileName = "log_" + new SimpleDateFormat("MM-dd'_'HH:mm:ss", Locale.US)
                        .format(new Date());
                return new FileOutputStream(new File(mDir, fileName + ".log"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void run() {
            FileOutputStream out = getOut();
            try {
                logcatProc = Runtime.getRuntime().exec(cmd);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()),
                        1024);
                String line;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }
                    if (out != null && line.contains(mPID)) {
                        byte[] temp = (line + "\n").getBytes();
                        length += temp.length;
                        out.write(temp);
                    }
                    if (length >= MAX_LENGTH) {
                        length = 0;
                        if (out != null) {
                            out.close();
                        }
                        out = getOut();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mRunning = false;
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
