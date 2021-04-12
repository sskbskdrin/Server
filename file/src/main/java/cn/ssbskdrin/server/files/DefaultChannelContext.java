package cn.ssbskdrin.server.files;

import cn.ssbskdrin.server.files.core.ChannelContext;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class DefaultChannelContext extends ChannelContext {

    @Override
    public void onReceive() throws Exception {
        int len = buffer.remaining();
        Log.d(new String(buffer.array(), 0, len));
        buffer.clear();
        buffer.put(("hello " + len).getBytes());
        buffer.flip();
        switchMode(false);
    }
}
