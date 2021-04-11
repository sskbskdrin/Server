package cn.ssbskdrin.server.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public abstract class ChannelContext implements Runnable, Log {

    private final ByteChannel channel;
    private NioServer server;
    private final Queue<ByteBuffer> queue = new LinkedBlockingQueue<>();
    private volatile boolean isClose = false;

    public ChannelContext(ByteChannel channel) {
        this.channel = channel;
        try {
            onActive();
        } catch (IOException e) {
            onException(e);
            close();
        }
    }

    void setServer(NioServer server) {
        this.server = server;
    }

    boolean tryRead() {
        try {
            ByteBuffer buff = ByteBufferUtil.obtain();
            int ret = channel.read(buff);
            if (ret > 0) {
                queue.offer(buff);
            } else if (ret == -1) {
                channel.close();
                close();
            }
            return ret > 0;
        } catch (IOException e) {
            onException(e);
            close();
            return false;
        }
    }

    @Override
    public final void run() {
        log(Thread.currentThread() + "channel read");
        try {
            if (isClose) {
                return;
            }
            while (!queue.isEmpty()) {
                ByteBuffer buffer = queue.poll();
                buffer.flip();
                onReceive(channel, buffer);
                ByteBufferUtil.recycle(buffer);
            }
            ByteBuffer buffer = ByteBufferUtil.obtain();
            int read = channel.read(buffer);
            while (read > 0) {
                buffer.flip();
                onReceive(channel, buffer);
                buffer.clear();
                read = channel.read(buffer);
            }
            ByteBufferUtil.recycle(buffer);
            if (read == -1) {
                channel.close();
                close();
            }
        } catch (Exception e) {
            onException(e);
            onClose();
        }
    }

    protected void onActive() throws IOException {
        log("onActive");
        channel.write(ByteBuffer.wrap("hello accept".getBytes()));
    }

    public abstract void onReceive(ByteChannel channel, ByteBuffer buffer) throws Exception;

    protected void onException(Exception e) {
        log("onException");
        e.printStackTrace();
    }

    private void close() {
        isClose = true;
        server.remove(this);
        onClose();
    }

    protected void onClose() {
        log("onClose");
    }

}
