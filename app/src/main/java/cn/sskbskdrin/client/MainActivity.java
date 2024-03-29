package cn.sskbskdrin.client;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import cn.sskbskdrin.server.rtmblib.Rtmp;
import cn.sskbskdrin.server.util.SLog;

/**
 * Created on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.main_file).setOnClickListener(this);
        findViewById(R.id.main_ftp).setOnClickListener(this);
        findViewById(R.id.main_http).setOnClickListener(this);
        findViewById(R.id.main_socket).setOnClickListener(this);
        findViewById(R.id.main_rtsp).setOnClickListener(this);
        findViewById(R.id.main_rtmp).setOnClickListener(this);
        String ip = getLocalIpAddress();
        ((TextView) findViewById(R.id.main_ip)).setText(ip.startsWith("0.") ? getPhoneIp() : ip);
        SLog.d(TAG, "main native " + Rtmp.getNativeString());
        SLog.d(TAG, getPackageCodePath());
        //        getFragmentManager().beginTransaction().add(new Frag(), "frag").commit();
        //        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts
        //        ("package",
        //            getPackageName(), (String) null));
        //        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_file:
                break;
            case R.id.main_ftp:
                start("ftp");
                break;
            case R.id.main_http:
                start("http");
                //                SimpleWebServer.main(new String[]{"-d", "/sdcard/"});
                break;
            case R.id.main_socket:
                start("socket");
                break;
            case R.id.main_rtsp:
                start("rtsp");
                break;
            case R.id.main_rtmp:
                start("rtmp");
                //                                Rtmp.init("rtmp://172.31.2.57");
                break;
            default:
        }
    }

    private void start(final String name) {
        new Thread(() -> {
            //            SimpleWebServer.main(new String[]{"-d", "/sdcard/"});
            Main.main(new String[]{name});
        }).start();
    }

    public String getLocalIpAddress() {
        // 获取WiFi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 判断WiFi是否开启
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + (ip >> 24 & 0xFF);
    }

    private String getPhoneIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        //if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}
