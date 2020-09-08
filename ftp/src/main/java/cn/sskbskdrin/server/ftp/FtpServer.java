package cn.sskbskdrin.server.ftp;

import cn.sskbskdrin.server.base.BaseServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author ex-keayuan001
 */
public class FtpServer extends BaseServer {

    private static FtpServer mInstance;

    private FtpServer() {
        TAG = "FtpServer";
    }

    public static FtpServer getInstance() {
        if (mInstance == null) {
            synchronized (FtpServer.class) {
                if (mInstance == null) {
                    mInstance = new FtpServer();
                }
            }
        }
        return mInstance;
    }

    public void start(String path, int port) {
        Share.rootDir = path;
        super.start(port);
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("encoder", new FtpEncoder());
        pipeline.addLast("encoder_byte", new MessageToByteEncoder<byte[]>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
                out.writeBytes(msg);
            }
        });
        pipeline.addLast("decoder", new FtpDecoder());
        pipeline.addLast("handler", new FtpClientChannel());
    }

}
