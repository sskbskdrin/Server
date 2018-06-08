package cn.sskbskdrin.server.ftp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author ex-keayuan001
 */
public class FtpDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        out.add(new String(data));
    }
}
