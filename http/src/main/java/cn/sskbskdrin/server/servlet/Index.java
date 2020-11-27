package cn.sskbskdrin.server.servlet;

import cn.sskbskdrin.server.annotation.API;
import cn.sskbskdrin.server.http.HandlerServlet;
import cn.sskbskdrin.server.util.Html;
import cn.sskbskdrin.server.util.ResponseFactory;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.Version;

/**
 * Created by keayuan on 2020/9/8.
 *
 * @author keayuan
 */
@API("/")
public class Index implements HandlerServlet {
    @Override
    public HttpResponse get(HttpRequest request) {
        String html = new Html().header(header -> {
            header.meta("charset=\"utf-8\"");
            header.title("HTTP");
        }).body(body -> {
            body.h1("Welcome to Http Server");
            body.p("version " + Version.identify(ClassLoader.getSystemClassLoader()));
        }).toString();
        return ResponseFactory.successHtml(html);
    }

    @Override
    public HttpResponse post(HttpRequest request) {
        return null;
    }
}
