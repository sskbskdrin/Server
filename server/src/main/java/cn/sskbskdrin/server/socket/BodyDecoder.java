package cn.sskbskdrin.server.socket;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class BodyDecoder extends ByteToMessageDecoder {
    private static final int OFFSET = 4;//head 0x0000FFFC ,flag 0x0000ffcf
    private static final int LENGTH = 4;//int
    private static final int CRC = 4;

    public BodyDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf byteBuf, List<Object> list) throws Exception {
        Body body = decode(context, byteBuf);
        if (body != null) {
            list.add(body);
        }
    }

    private Body decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }
        int ableLength = in.readableBytes();
        System.out.println("able length=" + ableLength);
        if (ableLength < OFFSET + LENGTH) {
            System.out.println("可读信息段比头部信息都小，你在逗我？");
            return null;
        }
        int begin = in.readerIndex();
        Body body = new Body();
        //注意在读的过程中，readIndex的指针也在移动
        int head = in.readInt();
        body.head = head >>> 16;

        body.flag = head & 0xffff;

        int length = in.readInt();

        if (ableLength < length + CRC) {
            in.readerIndex(begin);
            System.out.println("body字段你告诉我长度是" + length + ",但是真实情况是没有这么多，你又逗我？");
            return null;
        }
        byte[] buff = new byte[length];
        in.readBytes(buff);
        body.data = buff;
        body.crc = in.readInt();
        return body;
    }
}
