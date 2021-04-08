package cn.sskbskdrin.server.http;

import cn.sskbskdrin.server.base.BaseServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServer extends BaseServer {

    private static HttpServer mInstance;

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

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(8 * 1024 * 1024, true));
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast("handler", new HttpHandler());
    }
}
