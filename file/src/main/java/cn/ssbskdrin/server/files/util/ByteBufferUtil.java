package cn.ssbskdrin.server.files.util;

import java.nio.ByteBuffer;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class ByteBufferUtil {

    private static final int CACHE_SIZE = 8192;
    private static final Object sPoolSync = new Object();
    private static final int MAX_POOL_SIZE = 128;
    private static final ByteBuffer[] sPool = new ByteBuffer[MAX_POOL_SIZE];
    private static int sPoolSize = 0;

    public static ByteBuffer obtain() {
        synchronized (sPoolSync) {
            if (sPoolSize > 0) {
                return sPool[--sPoolSize];
            }
        }
        return ByteBuffer.allocate(CACHE_SIZE);
    }

    public static void recycle(ByteBuffer buffer) {
        if (buffer == null || buffer.capacity() != CACHE_SIZE) return;
        buffer.clear();
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                sPool[sPoolSize++] = buffer;
            }
        }
    }
}
