package cn.sskbskdrin.server.http;

import cn.sskbskdrin.server.base.BaseServer;
import cn.sskbskdrin.server.servlet.screenshot.ScreenShot;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServer extends BaseServer {

    private static HttpServer mInstance;

    static {
        HttpHandler.register("/screenshot", ScreenShot.class);
    }

    private HttpServer() {
        TAG = "HttpServer";
    }

    public static HttpServer getInstance() {
        if (mInstance == null) {
            synchronized (HttpServer.class) {
                if (mInstance == null) {
                    mInstance = new HttpServer();
                }
            }
        }
        return mInstance;
    }

    public void register(String path, Class<? extends HandlerServlet> clazz) {
        HttpHandler.register(path, clazz);
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024));
        pipeline.addLast("handler", new HttpHandler());
    }
}
