package cn.sskbskdrin.client;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import cn.sskbskdrin.log.Logger;
import cn.sskbskdrin.log.disk.DiskLogStrategy;
import cn.sskbskdrin.log.disk.DiskPrinter;
import cn.sskbskdrin.log.logcat.LogcatPrinter;
import cn.sskbskdrin.server.ftp.FtpServer;
import cn.sskbskdrin.server.ftp.Share;
import cn.sskbskdrin.server.http.HttpServer;
import cn.sskbskdrin.server.socket.NettyServer;

/**
 * Created on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public class MainActivity extends Activity implements View.OnClickListener {
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.main_ftp).setOnClickListener(this);
        findViewById(R.id.main_http).setOnClickListener(this);
        findViewById(R.id.main_socket).setOnClickListener(this);
        Logger.tag("ayke", "");
        Logger.addPinter(new LogcatPrinter());
        Logger.addPinter(new DiskPrinter(new DiskLogStrategy(getExternalFilesDir("log").getAbsolutePath(), 0), null));
        findViewById(R.id.main_ftp).postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.e("test" + count++);
                findViewById(R.id.main_ftp).postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_ftp:
                String path = getFilesDir().getAbsolutePath();
                Share.rootDir = path.substring(0, path.lastIndexOf("/"));
                Share.rootDir = "/sdcard";
                FtpServer.getInstance().start(2121);
                break;
            case R.id.main_http:
                HttpServer.getInstance().start(8080);
                break;
            case R.id.main_socket:
                NettyServer.getInstance().start(8088);
                break;
            default:
        }
    }
}
