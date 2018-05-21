package cn.sskbskdrin.client;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.sskbskdrin.server.http.HandlerServlet;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;

/**
 * Created by ex-keayuan001 on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public class Server implements HandlerServlet {
    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    @Override
    public HttpResponse get(HttpRequest request) {
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
                try {
                    bout.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    public HttpResponse post(HttpRequest request) {
        return null;
    }
}
