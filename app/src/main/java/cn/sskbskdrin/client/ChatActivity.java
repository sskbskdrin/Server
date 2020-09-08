package cn.sskbskdrin.client;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import io.netty.channel.Channel;

/**
 * @author ex-keayuan001
 */
public class ChatActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ChatActivity";

    private WindowManager mWindowManager;
    public static FloatView mLayout;

    private Channel mChannel;

    private EditText mHostView;
    private EditText mInputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
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
                Main.main(new String[]{"http"});
                break;
            case R.id.client_send:
                if (mChannel != null) {
                    String content = mInputView.getText().toString().trim();
                    mChannel.writeAndFlush(content);
                }
                break;
            default:
        }
    }
}
