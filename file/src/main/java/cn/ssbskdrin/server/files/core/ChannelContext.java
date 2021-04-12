package cn.ssbskdrin.server.files.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import cn.ssbskdrin.server.files.ByteBufferUtil;
import cn.ssbskdrin.server.files.Log;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public abstract class ChannelContext {

    private static final AtomicInteger sId = new AtomicInteger();
    private SelectionKey key;
    private SocketChannel channel;
    protected final ByteBuffer buffer = ByteBufferUtil.obtain();
    private final int id;

    public ChannelContext() {
        id = sId.getAndIncrement();
        try {
            onActive();
        } catch (IOException e) {
            onException(e);
        }
    }

    void setKey(SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
    }

    protected boolean isWriteComplete() {
        return !buffer.hasRemaining();
    }

    void write() {
        try {
            channel.write(buffer);
            if (isWriteComplete()) {
                switchMode(true);
            }
        } catch (IOException e) {
            onException(e);
        }
    }

    void read() {
        buffer.compact();
        try {
            int ret = channel.read(buffer);
            if (ret == -1) {
                close();
                return;
            }
            if (ret > 0) {
                onReceive();
            }
        } catch (Exception e) {
            onException(e);
        }
    }

    public void switchMode(boolean read) throws ClosedChannelException {
        channel.register(key.selector(), read ? SelectionKey.OP_READ : SelectionKey.OP_WRITE, this);
    }

    protected void close() {
        Log.v("close " + id);
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        key.cancel();
        onClose();
    }

    protected void onActive() throws IOException {
        Log.v("onActive " + id);
        //        channel.write(ByteBuffer.wrap("hello accept".getBytes()));
    }

    public abstract void onReceive() throws Exception;

    protected void onException(Exception e) {
        Log.w("onException " + id, e);
        close();
    }

    protected void onClose() {
        Log.v("onClose " + id);
    }

}
