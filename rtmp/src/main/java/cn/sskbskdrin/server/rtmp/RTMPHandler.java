package cn.sskbskdrin.server.rtmp;

import cn.sskbskdrin.server.util.SLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cn.sskbskdrin.server.rtmp.Util.encodeInt24;
import static cn.sskbskdrin.server.rtmp.Util.encodeInt32;
import static cn.sskbskdrin.server.rtmp.Util.encodeIntB;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class RTMPHandler extends SimpleChannelInboundHandler<RTMPPacket> {
    private static final String TAG = "RTMPHandler";

    private ChannelHandlerContext context;

    RTMPPacket lastPacket;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SLog.d(TAG, "channelActive: ");
    }

    void sendAcknowledgement(RTMPPacket lastpacket) {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x02;
        header.timestamp = 0;
        header.bodySize = 4;
        header.msgTypeId = RTMPPacket.MSG_TYPE_SERVER_BW;
        header.msgStreamId = 0;

        packet.body.data = new ByteList(4);
        encodeIntB(packet.body.data, 5000000);
        sendPacket(packet);
    }

    void sendToClientBW() {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x02;
        header.timestamp = 0;
        header.bodySize = 5;
        header.msgTypeId = RTMPPacket.MSG_TYPE_CLIENT_BW;
        header.msgStreamId = 0;

        packet.body.data = new ByteList(5);
        encodeIntB(packet.body.data, 5000000);
        packet.body.data.add((byte) 2);
        sendPacket(packet);
    }

    void sendSetChunkSize() {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x02;
        header.timestamp = 0;
        header.bodySize = 4;
        header.msgTypeId = RTMPPacket.MSG_TYPE_CHUNK_SIZE;
        header.msgStreamId = 0;

        packet.body.data = new ByteList(4);
        encodeIntB(packet.body.data, 4096);
        sendPacket(packet);
    }

    void sendConnectResult() {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x03;
        header.timestamp = 0;
        header.bodySize = 4;
        header.msgTypeId = RTMPPacket.MSG_TYPE_INVOKE;
        header.msgStreamId = 0;

        packet.body.amfObject = new AMF.AMFObject();
        packet.body.addProperty("_result");
        packet.body.addProperty(1);
        AMF.AMFObject object = new AMF.AMFObject();
        object.addProperty("fmsVer", "FMS/3,0,1,123");
        object.addProperty("capabilities", 31);
        packet.body.addProperty(object);
        object = new AMF.AMFObject();
        object.addProperty("level", "status");
        object.addProperty("code", "NetConnection.Connect.Success");
        object.addProperty("description", "Connection succeeded");
        object.addProperty("objectEncoding", 0);
        packet.body.addProperty(object);
        sendPacket(packet);
    }

    void sendCreateStreamResult(RTMPPacket last) {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x03;
        header.timestamp = 0;
        header.bodySize = 29;
        header.msgTypeId = RTMPPacket.MSG_TYPE_INVOKE;
        header.msgStreamId = 0;

        packet.body.amfObject = new AMF.AMFObject();
        packet.body.addProperty("_result");
        packet.body.addProperty(last.getTransactionId());
        packet.body.addPropertyNull();

        packet.body.addProperty(1);
        sendPacket(packet);
    }

    void sendUserControl(RTMPPacket request) {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = request.header.chunkStreamId;
        header.timestamp = 0;
        header.bodySize = 6;
        header.msgTypeId = RTMPPacket.MSG_TYPE_PING;
        header.msgStreamId = 0;

        packet.body.data = new ByteList();
        packet.body.data.add((byte) 0);
        packet.body.data.add((byte) 0);
        encodeInt32(packet.body.data, request.body.eventStreamId);
        sendPacket(packet);
    }

    void sendOnStatus(RTMPPacket request) {
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x05;
        header.timestamp = 0;
        header.bodySize = 6;
        header.msgTypeId = RTMPPacket.MSG_TYPE_INVOKE;
        header.msgStreamId = request.header.msgStreamId;

        packet.body.addProperty("onStatus");
        packet.body.addProperty(0);
        packet.body.addPropertyNull();

        AMF.AMFObject object = new AMF.AMFObject();
        object.addProperty("level", "status");
        object.addProperty("code", "NetStream.Play.Start");
        object.addProperty("description", "live");
        packet.body.addProperty(object);
        sendPacket(packet);
    }

    long startTime = -1;

    void sendVideoData(byte[] data) {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
        RTMPPacket packet = new RTMPPacket();
        RTMPPacket.Header header = packet.header;
        header.fmt = 0;
        header.chunkStreamId = 0x04;
        header.timestamp = (int) (System.currentTimeMillis() - startTime);
        startTime += header.timestamp;
        header.bodySize = 6;
        header.msgTypeId = RTMPPacket.MSG_TYPE_VIDEO_DATA;
        header.msgStreamId = 1;

        packet.body.data = new ByteList(data.length + 1);
        packet.body.data.add((byte) 0x27);
        packet.body.data.addAll(data);
    }

    void sendPacket(RTMPPacket packet) {
        ByteList data = new ByteList();
        SLog.i(TAG, "send packet=\n" + packet);
        if (packet.body.data == null) {
            AMF.encode(packet.body);
        }

        RTMPPacket.Header header = packet.header;
        header.bodySize = packet.body.data.size();
        int chunkStreamId = header.chunkStreamId;
        int basicHeaderLen = 1;
        if (chunkStreamId > 319) {
            basicHeaderLen = 3;
            chunkStreamId = 1;
        } else if (packet.header.chunkStreamId > 63) {
            basicHeaderLen = 2;
            chunkStreamId = 0;
        }

        byte temp = (byte) (header.fmt << 6 | (chunkStreamId & 0x3f));
        data.add(temp);
        if (basicHeaderLen >= 2) {
            int secondByte = header.chunkStreamId - 64;
            data.add((byte) secondByte);
            if (basicHeaderLen == 3) {
                data.add((byte) (secondByte >> 8));
            }
        }

        if (header.hasAbsTimestamp) {
            encodeInt24(data, 0xffffff);
        } else {
            encodeInt24(data, header.timestamp);
        }
        encodeInt24(data, header.bodySize);
        data.add(header.msgTypeId);
        encodeInt32(data, header.msgStreamId);
        if (header.hasAbsTimestamp) {
            encodeInt32(data, header.timestamp);
        }

        if (packet.body.data != null) {
            data.addAll(packet.body.data.getValue());
        }

        context.writeAndFlush(data);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RTMPPacket packet) {
        context = ctx;
        SLog.d(TAG, "channel read");
        if (packet == null) {
            SLog.w(TAG, "packet is null");
            return;
        }
        SLog.d(TAG, "packet=\n" + packet);
        if (packet.isCommand()) {
            switch (packet.getCommand()) {
                case "connect":
                    sendAcknowledgement(packet);
                    sendToClientBW();
                    sendSetChunkSize();
                    sendConnectResult();
                    break;
                case "createStream":
                    sendCreateStreamResult(packet);
                    break;
                case "getStreamLength":
                    break;
                case "play":
                    sendOnStatus(packet);
                    break;
                default:
            }
        } else if (packet.header.msgTypeId == RTMPPacket.MSG_TYPE_PING) {
            sendUserControl(packet);
        }
        lastPacket = packet;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SLog.e(TAG, "exceptionCaught: ");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SLog.w(TAG, "channelInactive: ");
    }
}
