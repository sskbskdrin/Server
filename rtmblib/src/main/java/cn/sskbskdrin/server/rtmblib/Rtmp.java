package cn.sskbskdrin.server.rtmblib;

/**
 * @author sskbskdrin
 * @date 2019/April/26
 */
public class Rtmp {
    static {
        System.loadLibrary("rtmp");
    }

    public native static String getNativeString();

    public static native void init(String url);
    public static native void release();

}
