package cn.sskbskdrin.http.servlet.screenshot;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ex-keayuan001 on 2018/2/7.
 *
 * @author ex-keayuan001
 */
public class Jump {

    private static Pixel[] USER = new Pixel[]{new Pixel(-1, 43, 43, 73), new Pixel(-1, 45, 45, 75), new Pixel(-1, 46,
        46, 81), new Pixel(-1, 50, 51, 89), new Pixel(-1, 54, 54, 94), new Pixel(-1, 55, 56, 97), new Pixel(-1, 55,
        56, 97), new Pixel(-1, 55, 55, 96), new Pixel(-1, 55, 55, 96), new Pixel(-1, 56, 55, 96), new Pixel(-1, 57,
        55, 95), new Pixel(-1, 57, 55, 95), new Pixel(-1, 57, 55, 95), new Pixel(-1, 57, 55, 90), new Pixel(-1, 56,
        54, 84)};

    private int width;
    private int height;

    public static String get(int l, int t, int w, int h) {
        Jump jump = new Jump();
        jump.width = w;
        jump.height = h;
        JSONObject result = new JSONObject();
        try {
            Bitmap bitmap = SysUtil.screenshot(l, t, w, h);
            Point[] points = get(bitmap, jump);
            if (points != null && points.length > 1) {
                JSONObject data = new JSONObject();
                JSONObject object = new JSONObject();
                if (points[0] != null) {
                    object.put("x", points[0].x);
                    object.put("y", points[0].y);
                }
                data.put("user", object);
                object = new JSONObject();
                if (points[1] != null) {
                    object.put("x", points[1].x);
                    object.put("y", points[1].y);
                }
                data.put("target", object);
                result.put("data", data);
            }
            result.put("code", 400);
            result.put("msg", "success");
        } catch (JSONException e) {
            e.printStackTrace();
            result = new JSONObject();
            try {
                result.put("code", 401);
                result.put("msg", "fail");
                result.put("data", null);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return result.toString();
    }

    private static Point[] get(Bitmap bitmap, Jump jump) {
        if (bitmap != null) {
            int[] pixel = new int[bitmap.getByteCount()];
            bitmap.getPixels(pixel, 0, jump.width, 0, 0, jump.width, jump.height);
            jump.width = bitmap.getWidth();
            jump.height = bitmap.getHeight();
            Pixel[][] pixels = new Pixel[jump.height][jump.width];
            int count = 0;
            for (int h = 0; h < jump.height; h++) {
                for (int w = 0; w < jump.width; w++) {
                    pixels[h][w] = new Pixel(pixel[++count]);
                    //                    System.out.println(pixels[h][w]);
                }
            }
            Point user = jump.findUser(pixels);
            int start = 0;
            int end = jump.width;
            if (user != null) {
                if (user.x - jump.width / 2 > 0) {
                    end = user.x - 38;
                } else {
                    start = user.x + 38;
                }
            }
            Point target = jump.findTarget(pixels, start, end);
            return new Point[]{user, target};
        }
        return null;
    }

    private Point findUser(Pixel[][] pixels) {
        for (int h = height - 1; h > 0; h -= 2) {
            for (int w = width - 1; w > 75; w -= 2) {
                if (isSimilar(pixels[h][w], USER[USER.length - 1], 5) && isUser(pixels, w, h)) {
                    return new Point(w - 38, h - 3);
                }
            }
        }
        return null;
    }

    private Point findTarget(Pixel[][] pixels, int start, int end) {
        Pixel color = pixels[10][width / 2];
        for (int h = 10; h < height; h += 2) {
            for (int w = start; w < end; w += 3) {
                if (!isSimilar(pixels[h][w], color, 10)) {
                    return new Point(w, findTargetY(pixels, w, h, end, color));
                }
            }
        }
        return new Point();
    }

    private int findTargetY(Pixel[][] pixels, int x, int y, int end, Pixel color) {
        int w = x;
        int h = y;
        int count = 0;
        while (h < y + 50) {
            while (w < end) {
                if (isSimilar(pixels[h][w], color, 10)) {
                    if (count++ >= 3) {
                        return h - count;
                    }
                    h++;
                } else {
                    count = 0;
                    w++;
                }
            }
            if (w == end) {
                break;
            }
        }
        return h;
    }

    private boolean isUser(Pixel[][] pixels, int x, int y) {
        for (int i = 14; i >= 0; i--) {
            if (!isSimilar(pixels[y][x - 5 * i], USER[USER.length - i - 1], 5)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSimilar(Pixel src, Pixel tar, int delta) {
        return Math.abs(src.r - tar.r) < delta && Math.abs(src.g - tar.g) < delta && Math.abs(src.b - tar.b) < delta;
    }

    private static class Pixel {
        private int a;
        private int r;
        private int g;
        private int b;

        protected Pixel(int a, int r, int g, int b) {
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        private Pixel(int color) {
            a = Color.alpha(color);
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
        }

        @Override
        public String toString() {
            return "new Pixel(" + a + "," + r + "," + g + "," + b + "),";
        }
    }
}
