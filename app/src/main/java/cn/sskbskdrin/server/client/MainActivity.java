package cn.sskbskdrin.server.client;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.netty.channel.Channel;
import cn.sskbskdrin.server.socket.NettyServer;

/**
 * @author ex-keayuan001
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private WindowManager mWindowManager;
    public static FloatView mLayout;

    private Channel mChannel;

    private EditText mHostView;
    private EditText mInputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHostView = (EditText) findViewById(R.id.client_ip);
        findViewById(R.id.client_connect).setOnClickListener(this);
        //		showView(true);
        //		showView(false);
        ImageView image = findViewById(R.id.image);
        Bitmap bitmap = Bitmap.createBitmap(2, 5, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                bitmap.setPixel(j, i, 0x80ffffff);
            }
        }
        image.setImageBitmap(bitmap);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        try {
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = outputStream.toByteArray();

        for (byte aByte : bytes) {
            System.out.print(Integer.toHexString(aByte & 0xff) + " ");
        }
        System.out.println();
    }

    private void showView(boolean touch) {
        mLayout = new FloatView(getApplicationContext());
        mLayout.setBackgroundResource(R.mipmap.ic_launcher);
        // 获取WindowManager
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        // 设置LayoutParams(全局变量）相关参数
        WindowManager.LayoutParams param = new WindowManager.LayoutParams();

        param.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // 系统提示类型,重要
        param.format = 1;
        param.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; // 不能抢占聚焦点
        if (touch) {
            param.flags = param.flags | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;
            param.flags = param.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
        param.flags = param.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS; // 排版不受限制

        param.alpha = 1.0f;

        param.gravity = Gravity.LEFT | Gravity.TOP; // 调整悬浮窗口至左上角
        // 以屏幕左上角为原点，设置x、y初始值
        param.x = 0;
        param.y = 0;

        // 设置悬浮窗口长宽数据
        if (touch) {
            param.width = 240;
            param.height = 240;
        } else {
            param.width = 340;
            param.height = 340;
        }
        // 显示myFloatView图像
        mLayout.setWmParams(param);
        mWindowManager.addView(mLayout, param);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.client_connect:
                String string = mHostView.getText().toString().trim();
                if (!TextUtils.isEmpty(string)) {
                    final String[] str = string.split(":");
                    if (str.length != 2) {
                        return;
                    }
                    final String host = str[0];
                    final int port = Integer.parseInt(str[1]);
                    NettyServer.start(port);
                    //                    NettyClient.connect(str[0], Integer.parseInt(str[1]), new NettyClient
                    // .ChannelCallback() {
                    //                        @Override
                    //                        public void onConnect(Channel channel) {
                    //                            mChannel = channel;
                    //                        }
                    //
                    //                        @Override
                    //                        public void onDisconnect(Channel channel) {
                    //                            mChannel = null;
                    //                        }
                    //                    });
                }
                break;
            case R.id.client_send:
                if (mChannel != null) {
                    String content = mInputView.getText().toString().trim();
                    mChannel.writeAndFlush(content);
                }
                break;
        }
    }
}
