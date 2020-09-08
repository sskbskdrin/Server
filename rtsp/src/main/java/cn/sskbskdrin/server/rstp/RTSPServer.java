package cn.sskbskdrin.server.rstp;

import cn.sskbskdrin.server.util.SLog;
import cn.sskbskdrin.server.base.BaseServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;

/**
 * @author sskbskdrin
 * @date 2019/April/23
 */
public class RTSPServer extends BaseServer {
    private static RTSPServer mInstance;

    private RTSPServer() {
        TAG = "RTSPServer";
    }

    public static RTSPServer getInstance() {
        if (mInstance == null) {
            synchronized (RTSPServer.class) {
                if (mInstance == null) {
                    mInstance = new RTSPServer();
                }
            }
        }
        return mInstance;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new RtspDecoder());
        pipeline.addLast("encoder", new RtspEncoder());

        pipeline.addLast("handler", new Handler());
    }

    private static class Handler extends SimpleChannelInboundHandler<HttpRequest> {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            SLog.d(TAG, "channelActive");
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
            SLog.d(TAG, "channel read");
            if (msg == null) {
                ctx.writeAndFlush("200 welcome to rtsp\r\n");
                return;
            }
            SLog.i(TAG, msg.toString());
            HttpResponse response = RTSPHandler.handler(msg);
            if (response != null) {
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush("400 welcome to rtsp\r\n");
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            SLog.i(TAG, "channelWritabilityChanged:" + ctx.channel().isWritable());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            SLog.e(TAG, "exceptionCaught: ", cause);
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            SLog.w(TAG, "channelInactive: ");
        }
    }
}
