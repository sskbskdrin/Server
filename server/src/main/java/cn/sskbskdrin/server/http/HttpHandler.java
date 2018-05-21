package cn.sskbskdrin.server.http;

import java.util.Map;

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

    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    static Map<String, Class<? extends HandlerServlet>> mMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        HttpResponse response = null;
        QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());
        if (mMap != null && mMap.containsKey(decoder.path())) {
            HandlerServlet servlet = mMap.get(decoder.path()).newInstance();
            if (HttpMethod.GET.compareTo(msg.method()) == 0) {
                response = servlet.get(msg);
            } else if (HttpMethod.POST.compareTo(msg.method()) == 0) {
                response = servlet.post(msg);
            }
        }
        if (response == null) {
            ByteBuf content = Unpooled.wrappedBuffer("{\"code\": 400,\"msg\": \"system error\",\"data\": null}"
                .getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelReadComplete");
        ctx.flush();
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if (null != cause) {
            cause.printStackTrace();
        }
        ctx.close();
    }
}