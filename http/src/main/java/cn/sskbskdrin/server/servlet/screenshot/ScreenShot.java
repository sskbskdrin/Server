package cn.sskbskdrin.server.servlet.screenshot;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import cn.sskbskdrin.server.http.HandlerServlet;
import cn.sskbskdrin.server.util.SLog;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

/**
 * Created by ex-keayuan001 on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public class ScreenShot implements HandlerServlet {
    private static final String TAG = "ScreenShot";
    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    @Override
    public HttpResponse get(HttpRequest request) {
        DefaultFullHttpResponse response = null;
        String uri = request.uri();
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = decoder.parameters();
        System.out.println("path=" + decoder.path());
        if ("/screenshot".equals(decoder.path())) {
            List<String> list = params.get("l");
            int l = 0, t = 0, h = 0, w = 0;
            if (list != null && list.size() > 0) {
                l = SysUtil.parseInt(list.get(0));
            }
            list = params.get("t");
            if (list != null && list.size() > 0) {
                t = SysUtil.parseInt(list.get(0));
            }
            list = params.get("h");
            if (list != null && list.size() > 0) {
                h = SysUtil.parseInt(list.get(0));
            }
            list = params.get("w");
            if (list != null && list.size() > 0) {
                w = SysUtil.parseInt(list.get(0));
            }

            Bitmap bitmap = Shot.getInstance().screenshot(l, t, w, h);
            byte[] bytes;
            if (bitmap == null) {
                bytes = "bitmap is null".getBytes();
            } else {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bout);
                try {
                    bout.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bytes = bout.toByteArray();
            }
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes));
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else if ("/jump".equals(decoder.path())) {
            List<String> list = params.get("l");
            int l = 0, t = 0, h = 0, w = 0;
            if (list != null && list.size() > 0) {
                l = SysUtil.parseInt(list.get(0));
            }
            list = params.get("t");
            if (list != null && list.size() > 0) {
                t = SysUtil.parseInt(list.get(0));
            }
            list = params.get("h");
            if (list != null && list.size() > 0) {
                h = SysUtil.parseInt(list.get(0));
            }
            list = params.get("w");
            if (list != null && list.size() > 0) {
                w = SysUtil.parseInt(list.get(0));
            }
            //            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled
            //                    .wrappedBuffer(Jump.get(l, t, w, h).getBytes()));
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        return response;
    }

    @Override
    public HttpResponse post(HttpRequest request) {
        return null;
    }

    private static class Util {

    }

    private static class Shot {

        static Shot getInstance() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return new ShotP();
            }
            return new Shot();
        }

        Class getSurfaceClass() {
            String surfaceClassName;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                surfaceClassName = "android.view.Surface";
            } else {
                surfaceClassName = "android.view.SurfaceControl";
            }
            try {
                return Class.forName(surfaceClassName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        Bitmap getBitmap(Class clz, Point size) {
            if (clz != null) {
                try {
                    Method method = clz.getDeclaredMethod("screenshot", new Class[]{int.class, int.class});
                    return (Bitmap) method.invoke(null, size.x, size.y);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        Bitmap screenshot(int left, int top, int width, int height) {
            SLog.d(TAG, "screenshot: l=" + left + " t=" + top + " w=" + width + " h=" + height);
            try {
                Point size = new Point(1920, 1080);
                //            SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
                Class clazz = getSurfaceClass();
                long start = System.currentTimeMillis();
                Bitmap b = getBitmap(clazz, size);
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
    }

    private static class ShotP extends Shot {
        @Override
        Bitmap getBitmap(Class clz, Point size) {
            if (clz != null) {
                try {
                    Method method = clz.getDeclaredMethod("screenshot", new Class[]{Rect.class, int.class, int.class,
                        int.class});
                    return (Bitmap) method.invoke(null, new Rect(0, 0, size.x, size.y), size.x, size.y, 90);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
