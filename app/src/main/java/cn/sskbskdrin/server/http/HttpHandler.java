package cn.sskbskdrin.server.http;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.sskbskdrin.server.http.business.Jump;
import cn.sskbskdrin.server.utils.SysUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

/**
 * Created by sskbskdrin on 2018/一月/27.
 */

public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        DefaultHttpResponse response = null;
        if (HttpMethod.GET.compareTo(msg.method()) == 0) {
            response = get(msg);
        } else if (HttpMethod.POST.compareTo(msg.method()) == 0) {
            response = post(msg);
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

    private DefaultHttpResponse get(FullHttpRequest request) throws IOException {
        DefaultFullHttpResponse response = null;
        String uri = request.uri();
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = decoder.parameters();
        if ("/screenshot".equals(decoder.path())) {
            List<String> list = params.get("l");
            int l = 0, t = 0, h = 0, w = 0;
            if (list != null && list.size() > 0) {
                l = SysUtil.parseInt(list.get(0));
            }
            list = params.get("t");
            if (list != null && list.size() > 0) {
                t = SysUtil.parseInt(list.get(0));
            }
            list = params.get("h");
            if (list != null && list.size() > 0) {
                h = SysUtil.parseInt(list.get(0));
            }
            list = params.get("w");
            if (list != null && list.size() > 0) {
                w = SysUtil.parseInt(list.get(0));
            }

            Bitmap bitmap = SysUtil.screenshot(l, t, w, h);
            byte[] bytes;
            if (bitmap == null) {
                bytes = "bitmap is null".getBytes();
            } else {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bout);
                bout.flush();
                bytes = bout.toByteArray();
            }
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled
                    .wrappedBuffer(bytes));
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else if ("/jump".equals(decoder.path())) {
            List<String> list = params.get("l");
            int l = 0, t = 0, h = 0, w = 0;
            if (list != null && list.size() > 0) {
                l = SysUtil.parseInt(list.get(0));
            }
            list = params.get("t");
            if (list != null && list.size() > 0) {
                t = SysUtil.parseInt(list.get(0));
            }
            list = params.get("h");
            if (list != null && list.size() > 0) {
                h = SysUtil.parseInt(list.get(0));
            }
            list = params.get("w");
            if (list != null && list.size() > 0) {
                w = SysUtil.parseInt(list.get(0));
            }
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled
                    .wrappedBuffer(Jump.get(l, t, w, h).getBytes()));
            HttpHeaders heads = response.headers();
            heads.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        return response;
    }

    public DefaultHttpResponse post(FullHttpRequest request) {
        return null;
    }
}