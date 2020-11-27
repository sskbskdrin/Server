package cn.sskbskdrin.server.http;

import cn.sskbskdrin.server.util.ResponseFactory;
import cn.sskbskdrin.server.util.SLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * @author sskbskdrin
 * @date 2018/一月/27
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final String TAG = "HttpHandler";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpResponse response = null;
        QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());
        SLog.d(TAG, msg.uri());
        if (decoder.path().startsWith("/files")) {
            HandlerServlet servlet = Route.route("/files");
            if (HttpMethod.GET.compareTo(msg.method()) == 0) {
                response = servlet.get(msg);
            } else if (HttpMethod.POST.compareTo(msg.method()) == 0) {
                response = servlet.post(msg);
            }
        } else {
            HandlerServlet servlet = Route.route(decoder.path());
            if (servlet != null) {
                if (HttpMethod.GET.compareTo(msg.method()) == 0) {
                    response = servlet.get(msg);
                } else if (HttpMethod.POST.compareTo(msg.method()) == 0) {
                    response = servlet.post(msg);
                }
            }
        }
        if (response == null) {
            response = ResponseFactory.error("-1", "system error");
        }
        ctx.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        SLog.d(TAG, "channelReadComplete");
        ctx.flush();
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SLog.w(TAG, "exceptionCaught");
        if (null != cause) {
            cause.printStackTrace();
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        SLog.w(TAG, "channelInactive: ");
    }
}