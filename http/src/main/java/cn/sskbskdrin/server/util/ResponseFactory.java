package cn.sskbskdrin.server.util;

import org.json.JSONException;
import org.json.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Created by keayuan on 2020/9/9.
 *
 * @author keayuan
 */
public class ResponseFactory {
    public static HttpResponse successHtml(String text) {
        ByteBuf content = Unpooled.wrappedBuffer(text.getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        heads.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }

    public static HttpResponse successJson(JSONObject obj) {
        ByteBuf content = Unpooled.wrappedBuffer(obj.toString().getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        heads.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }

    public static HttpResponse code(String code, String msg) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("code", code);
            obj.put("msg", msg);
            obj.put("data", null);
        } catch (JSONException ignored) {
        }
        ByteBuf content = Unpooled.wrappedBuffer(obj.toString().getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        heads.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }


}
