package cn.sskbskdrin.server.http;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by ex-keayuan001 on 2018/5/21.
 *
 * @author ex-keayuan001
 */
public interface HandlerServlet {

    HttpResponse get(HttpRequest request);

    HttpResponse post(HttpRequest request);

}
