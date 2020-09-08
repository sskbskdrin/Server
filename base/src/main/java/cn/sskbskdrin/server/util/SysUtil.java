package cn.sskbskdrin.server.util;

/**
 * Created by sskbskdrin on 2018/一月/27.
 */
public class SysUtil {
    private static final String TAG = "SysUtil:";

    public static int parseInt(String str) {
        if (str == null || str.length() == 0) {
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
