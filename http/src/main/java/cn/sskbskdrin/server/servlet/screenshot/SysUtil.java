package cn.sskbskdrin.server.servlet.screenshot;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;

import cn.sskbskdrin.server.util.SLog;

/**
 * Created by sskbskdrin on 2018/一月/27.
 */

public class SysUtil {
    private static final String TAG = "SysUtil:";

    public static Bitmap screenshot(int left, int top, int width, int height) {
        SLog.d(TAG, "screenshot: l=" + left + " t=" + top + " w=" + width + " h=" + height);
        try {
            Class cls = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = cls.getDeclaredMethod("getService", String.class);

            String surfaceClassName;
            Point size = new Point(1920, 1080);
            //            SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                surfaceClassName = "android.view.Surface";
            } else {
                surfaceClassName = "android.view.SurfaceControl";
            }
            Method method;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                method = Class.forName(surfaceClassName).getDeclaredMethod("screenshot", new Class[]{Rect.class,
                    int.class, int.class, int.class});
            } else {
                method = Class.forName(surfaceClassName).getDeclaredMethod("screenshot", new Class[]{int.class,
                    int.class});
            }

            long start = System.currentTimeMillis();
            Bitmap b = (Bitmap) method.invoke(null, size.x, size.y);
            SLog.d(TAG, "time=" + (System.currentTimeMillis() - start));
            int rotation = 1;

            Matrix m = new Matrix();
            if (rotation == 1) {
                m.postRotate(-90.0f);
            } else if (rotation == 2) {
                m.postRotate(-180.0f);
            } else if (rotation == 3) {
                m.postRotate(-270.0f);
            }
            if (width <= 0 || width > size.x) {
                width = size.x;
            }
            if (left < 0 || left > size.x) {
                left = 0;
            } else {
                if (left + width > size.x) {
                    width = size.x - left;
                }
            }
            if (height <= 0 || height > size.y) {
                height = size.y;
            }
            if (top < 0 || top > size.y) {
                top = 0;
            } else {
                if (top + height > size.y) {
                    height = size.y - top;
                }
            }
            SLog.d(TAG, "bitmap l=" + left + " t=" + top + " w=" + width + " h=" + height);
            return Bitmap.createBitmap(b, left, top, width, height, m, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap screntshot() {
        return screenshot(0, 0, 0, 0);
    }

    public static int parseInt(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i == 0) {
                if (chars[i] == '-') {
                    continue;
                }
            }
            if (chars[i] > '9' || chars[i] < '0') {
                return 0;
            }
        }
        return Integer.parseInt(str);
    }
}
