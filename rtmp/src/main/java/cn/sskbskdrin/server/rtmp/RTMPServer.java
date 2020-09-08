package cn.sskbskdrin.server.rtmp;

import cn.sskbskdrin.server.base.BaseServer;
import cn.sskbskdrin.server.util.SLog;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author sskbskdrin
 * @date 2019/April/23
 */
public class RTMPServer extends BaseServer {
    private static RTMPServer mInstance;

    private RTMPServer() {
        TAG = "RTMPServer";
    }

    public static RTMPServer getInstance() {
        if (mInstance == null) {
            synchronized (RTMPServer.class) {
                if (mInstance == null) {
                    mInstance = new RTMPServer();
                }
            }
        }
        return mInstance;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new RTMPDecoder());
        pipeline.addLast(new Encoder());

        pipeline.addLast(new RTMPHandler());
        pipeline.addLast(new Handler());
    }

    public class Encoder extends MessageToByteEncoder<ByteList> {

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteList msg, ByteBuf out) {
            out.writeBytes(msg.getValue(), 0, msg.size());
        }
    }

    class Handler extends SimpleChannelInboundHandler<byte[]> {
        private static final String TAG = "Handler";

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            SLog.d(TAG, "channelRead0: ");
            ctx.writeAndFlush(new ByteList(msg));
        }
    }
}
