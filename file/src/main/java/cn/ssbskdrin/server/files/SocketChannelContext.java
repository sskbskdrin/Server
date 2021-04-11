package cn.ssbskdrin.server.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class SocketChannelContext extends ChannelContext {

    public SocketChannelContext(ByteChannel channel) {
        super(channel);
    }

    protected void onActive() throws IOException {
        log("onActive");
    }

    @Override
    public void onReceive(ByteChannel channel, ByteBuffer buffer) throws Exception {
        logP(new String(buffer.array(), 0, buffer.remaining()));
    }

    protected void onException(Exception e) {
        log("onException");
        e.printStackTrace();
    }


    protected void onClose() {
        log("onClose");
    }

}
