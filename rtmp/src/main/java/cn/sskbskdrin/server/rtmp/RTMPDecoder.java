package cn.sskbskdrin.server.rtmp;

import java.util.Arrays;
import java.util.List;

import cn.sskbskdrin.server.util.SLog;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import static cn.sskbskdrin.server.rtmp.Util.printBytes;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class RTMPDecoder extends ByteToMessageDecoder {
    private static final String TAG = "RTMPDecoder";
    private byte[] buffer;

    private RTMPHandshakeSimple handshakeS;
    private boolean handshakeSuccess;

    private RTMPPacket lastPacket;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int ableLength = in.readableBytes();
        SLog.i(TAG, "able length=" + ableLength);
        if (!handshakeSuccess) {
            if (handshakeS == null) {
                handshakeS = new RTMPHandshakeSimple();
            }
            if (handshakeS.C0 == 0) {
                if (ableLength > 0) {
                    handshakeS.C0 = in.readByte();
                    SLog.i(TAG, "read C0");
                }
            }
            ableLength = in.readableBytes();
            if (!handshakeS.c1) {
                if (ableLength >= 1536) {
                    in.readBytes(handshakeS.C1);
                    handshakeS.timestampC1 |= handshakeS.C1[0];
                    handshakeS.timestampC1 |= handshakeS.C1[1] << 8;
                    handshakeS.timestampC1 |= handshakeS.C1[2] << 16;
                    handshakeS.timestampC1 |= handshakeS.C1[3] << 24;
                    handshakeS.timestampS1 = (int) System.currentTimeMillis();
                    handshakeS.c1 = true;
                    SLog.i(TAG, "read C1");

                    byte[] temp = new byte[3073];
                    temp[0] = 3;
                    System.arraycopy(generateS1(handshakeS.C1, handshakeS.timestampS1, 0), 0, temp, 1, 1536);
                    System.arraycopy(generateS1(handshakeS.C1, handshakeS.timestampS1, handshakeS.timestampC1), 0,
                        temp, 1537, 1536);
                    SLog.i(TAG, "write s0+s1+s2");
                    out.add(temp);
                }
            }

            ableLength = in.readableBytes();
            if (!handshakeS.c2 && ableLength >= 1536) {
                handshakeS.c2 = true;
                byte[] data = new byte[ableLength];
                in.readBytes(data);
                SLog.i(TAG, "read C2");
                handshakeSuccess = true;
            }
        } else {
            if (ableLength > 0) {
                SLog.i(TAG, "read data");
                int bufferLen = 0;
                if (buffer != null) {
                    bufferLen = buffer.length;
                }
                byte[] data;
                if (bufferLen > 0) {
                    data = Arrays.copyOf(buffer, ableLength + bufferLen);
                } else {
                    data = new byte[ableLength];
                }
                in.readBytes(data, bufferLen, ableLength);
                printBytes(data);

                int index;
                do {
                    RTMPPacket packet = new RTMPPacket();
                    index = packet.decode(data, lastPacket);
                    if (index < 0) {
                        buffer = data;
                        break;
                    } else if (index == 0 || index == data.length) {
                        buffer = null;
                        lastPacket = packet;
                        out.add(packet);
                        break;
                    } else {
                        byte[] temp = new byte[data.length - index];
                        System.arraycopy(data, index, temp, 0, temp.length);
                        index = 0;
                        data = temp;
                        lastPacket = packet;
                        out.add(packet);
                    }
                } while (index < data.length);
            }
        }
    }

    private static byte[] generateS1(byte[] c1, int time, int time2) {
        byte[] s1 = Arrays.copyOf(c1, c1.length);
        s1[0] = (byte) (time & 0xff);
        s1[1] = (byte) ((time >> 8) & 0xff);
        s1[2] = (byte) ((time >> 16) & 0xff);
        s1[3] = (byte) ((time >> 24) & 0xff);

        s1[4] = (byte) (time2 & 0xff);
        s1[5] = (byte) ((time2 >> 8) & 0xff);
        s1[6] = (byte) ((time2 >> 16) & 0xff);
        s1[7] = (byte) ((time2 >> 24) & 0xff);
        if (time2 != 0) {
            s1[10] = (byte) (s1[10] ^ 0xff);
        }
        return s1;
    }
}
