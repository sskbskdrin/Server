package cn.ssbskdrin.server.files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
class DefaultChannelContext extends ChannelContext {

    public DefaultChannelContext(ByteChannel channel) {
        super(channel);
    }

    @Override
    public void onReceive(ByteChannel channel, ByteBuffer buffer) throws IOException {
        int len = buffer.remaining();
        log(new String(buffer.array(), 0, len));
        buffer.clear();
        buffer.put(("hello " + len).getBytes());
        buffer.flip();
        channel.write(buffer);
    }
}
