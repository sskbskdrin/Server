package cn.sskbskdrin.server.socket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class BodyChannelInitializer extends ChannelInitializer<SocketChannel> {
	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		System.out.println("initChannel");
		ChannelPipeline pipeline = socketChannel.pipeline();
		pipeline.addLast("decoder", new BodyDecoder());
		pipeline.addLast("encoder", new BodyEncoder());
		pipeline.addLast("handler", new EchoHandler());
	}
}
