package cn.sskbskdrin.http.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by ex-keayuan001 on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public interface HandlerServlet {

    HttpResponse get(ChannelHandlerContext ctx, HttpRequest request);

    HttpResponse post(ChannelHandlerContext ctx, HttpRequest request);

}
