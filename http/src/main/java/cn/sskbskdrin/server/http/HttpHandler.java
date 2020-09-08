package cn.sskbskdrin.server.http;

import java.util.HashMap;
import java.util.Map;

import cn.sskbskdrin.server.util.SLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

/**
 * @author sskbskdrin
 * @date 2018/一月/27
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final String TAG = "HttpHandler";

    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    private static final Map<String, Class<? extends HandlerServlet>> mMap = new HashMap<>(16);

    public static void register(String path, Class<? extends HandlerServlet> clazz) {
        mMap.put(path, clazz);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpResponse response = null;
        QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());
        if (decoder.path() == null || decoder.path().length() <= 1) {
            ByteBuf content = Unpooled.wrappedBuffer("Welcome to Http Server".getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else {
            if (mMap.containsKey(decoder.path())) {
                HandlerServlet servlet = mMap.get(decoder.path()).newInstance();
                if (HttpMethod.GET.compareTo(msg.method()) == 0) {
                    response = servlet.get(msg);
                } else if (HttpMethod.POST.compareTo(msg.method()) == 0) {
                    response = servlet.post(msg);
                }
            }
        }
        if (response == null) {
            ByteBuf content =
                Unpooled.wrappedBuffer("{\"code\": 400,\"msg\": \"system error\",\"data\": null}".getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
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
        SLog.d(TAG, "exceptionCaught");
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