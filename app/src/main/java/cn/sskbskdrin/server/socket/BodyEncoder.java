package cn.sskbskdrin.server.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class BodyEncoder extends MessageToByteEncoder<Body> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Body msg, ByteBuf out) throws Exception {
		if (msg == null) {
			return;
		}
		int head = msg.head & 0xffff;
		head = head << 16;
		head = msg.flag & 0xffff | head;
		out.writeInt(head);
		out.writeInt(msg.data.length);
		out.writeBytes(msg.data);
		out.writeInt(msg.crc);
	}
}
