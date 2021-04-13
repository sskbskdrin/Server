package cn.ssbskdrin.server.files.http;

/**
 * Created by sskbskdrin on 2021/4/13.
 *
 * @author sskbskdrin
 */
class KMP {

    private final int[] next;
    private final byte[] dest;

    public KMP(byte[] dest) {
        this.dest = dest;
        next = kmpNext(dest);
    }

    public int find(byte[] src) {
        if (dest == null || dest.length == 0) return 0;
        if (src.length < dest.length) return -1;
        if (dest.length == 1) {
            for (int i = 0; i < src.length; i++) {
                if (i == dest[0]) return i;
            }
            return -1;
        }
        int i = 0;
        int j = 0;
        while (i < src.length) {
            if (j < 0 || src[i] == dest[j]) {
                ++i;
                ++j;
                if (j == dest.length) break;
            } else {
                j = next[j];
            }
        }
        if (j == dest.length) {
            return i - j;
        }
        return -1;
    }

    public static int kmpIndex(byte[] src, byte[] dest) {
        if (dest == null || dest.length == 0) return 0;
        if (src.length < dest.length) return -1;
        if (dest.length == 1) {
            for (int i = 0; i < src.length; i++) {
                if (i == dest[0]) return i;
            }
            return -1;
        }
        int[] next = kmpNext(dest);
        int i = 0;
        int j = 0;
        while (i < src.length) {
            if (j < 0 || src[i] == dest[j]) {
                ++i;
                ++j;
                if (j == dest.length) break;
            } else {
                j = next[j];
            }
        }
        if (j == dest.length) {
            return i - j;
        }
        return -1;
    }

    private static int[] kmpNext(byte[] src) {
        if (src == null || src.length == 0) return null;
        if (src.length == 1) return new int[-1];
        int[] next = new int[src.length];
        next[0] = -1;
        next[1] = 0;
        int i = 0;
        int j = -1;
        while (i < src.length - 1) {
            if (j < 0 || src[i] == src[j]) {
                ++i;
                ++j;
                next[i] = j >= 0 && src[i] == src[j] ? next[j] : j;
                //                next[i] = j;
            } else {
                j = next[j];
            }
        }
        return next;
    }

}
