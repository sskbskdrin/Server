package cn.sskbskdrin.server.http;

import android.util.Log;

import cn.sskbskdrin.server.base.BaseServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

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
        Log.d(TAG, "initChannel: ");
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024));
        pipeline.addLast("handler", new HttpHandler());
    }
}
