package cn.sskbskdrin.server.rtmp;

import static cn.sskbskdrin.server.rtmp.Util.decodeByte;
import static cn.sskbskdrin.server.rtmp.Util.decodeInt16;
import static cn.sskbskdrin.server.rtmp.Util.decodeInt24;
import static cn.sskbskdrin.server.rtmp.Util.decodeInt32;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class RTMPPacket {
    private static final int[] MESSAGE_HEAD_LEN = new int[]{11, 7, 3, 0};
    public static final byte MSG_TYPE_CHUNK_SIZE = 0x01;
    public static final byte MSG_TYPE_UNKNOWN = 0x02;
    public static final byte MSG_TYPE_BYTES_READ = 0x03;
    public static final byte MSG_TYPE_PING = 0x04;
    public static final byte MSG_TYPE_SERVER_BW = 0x05;
    public static final byte MSG_TYPE_CLIENT_BW = 0x06;
    public static final byte MSG_TYPE_AUDIO_DATA = 0x08;
    public static final byte MSG_TYPE_VIDEO_DATA = 0x09;
    public static final byte MSG_TYPE_NOTIFY = 0x12;
    public static final byte MSG_TYPE_SHARE_OBJ = 0x13;
    public static final byte MSG_TYPE_INVOKE = 0x14;

    int AMFMaxSize = 128;

    Header header = new Header();
    Body body = new Body();

    boolean isCommand() {
        return header.msgTypeId == MSG_TYPE_INVOKE;
    }

    String getCommand() {
        return body.amfObject.list.get(0).sValue;
    }

    int getTransactionId() {
        return (int) body.amfObject.list.get(1).nValue;
    }

    class Header {
        byte fmt;//0xc0
        int chunkStreamId;//0x3f

        int timestamp;//3byte
        int bodySize;//3byte
        byte msgTypeId;//1byte
        int msgStreamId;//4byte

        boolean hasAbsTimestamp;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("RTMP Header\n");
            builder.append("\tformat:").append(fmt).append('\n');
            builder.append("\tChunk Stream ID:").append(chunkStreamId).append('\n');
            builder.append("\tTimestamp:").append(hasAbsTimestamp ? "0xffffff" : timestamp).append('\n');
            builder.append("\tBody size:").append(bodySize).append('\n');
            builder.append("\tType Id:").append(msgTypeId).append('\n');
            builder.append("\tStream Id:").append(msgStreamId).append('\n');
            if (hasAbsTimestamp) {
                builder.append("exTimestamp:").append(timestamp).append('\n');
            }
            return builder.toString();
        }
    }

    class Body {
        ByteList data;
        /**
         * AMF数据
         */
        AMF.AMFObject amfObject;
        /**
         * 通知窗口大小数据
         */
        int WindowAcknowledgementSize;

        /**
         * UserControlMessage
         */
        int eventType;
        int eventStreamId;
        int eventBufferLen;

        public void addProperty(boolean value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.type = AMF.AMFDataType.AMF_BOOLEAN;
            property.bValue = value;
            addProperty(property);
        }

        public void addProperty(String value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.type = AMF.AMFDataType.AMF_STRING;
            property.sValue = value;
            addProperty(property);
        }

        public void addProperty(double value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.type = AMF.AMFDataType.AMF_NUMBER;
            property.nValue = value;
            addProperty(property);
        }

        public void addProperty(AMF.AMFObject value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.type = AMF.AMFDataType.AMF_OBJECT;
            property.oValue = value;
            addProperty(property);
        }

        public void addProperty(AMF.AMFObjectProperty property) {
            if (amfObject == null) {
                amfObject = new AMF.AMFObject();
            }
            amfObject.list.add(property);
        }

        public void addPropertyNull() {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.type = AMF.AMFDataType.AMF_NULL;
            addProperty(property);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("RTMP Body");
            if (data != null) {
                builder.append("\n\tbyte=");
                for (byte b : data.getValue()) {
                    String v = Integer.toHexString(b & 0xff);
                    if (v.length() == 1) {
                        builder.append('0');
                    }
                    builder.append(v);
                    builder.append(' ');
                }
            } else if (amfObject != null) {
                builder.append('\n');
                builder.append(amfObject.toString());
            } else if (WindowAcknowledgementSize != 0) {
                builder.append('\n');
                builder.append("\tWindow Acknowledgement Size:");
                builder.append(WindowAcknowledgementSize);
            } else {
                builder.append(" is empty!!!");
            }
            return builder.toString();
        }
    }

    /**
     * @return 返回0表示解码成功且结束，返回-1表示解码失败，数据不完整，返回>0表示解码成功，数据还有剩余
     */
    int decode(byte[] data, RTMPPacket lastPacket) {
        //Header
        int offset = 0;
        header.fmt = (byte) ((data[offset] & 0xc0) >> 6);
        header.chunkStreamId = data[offset] & 0x3f;
        offset++;
        try {
            if (header.chunkStreamId == 0) {
                header.chunkStreamId = decodeByte(data, offset);
                header.chunkStreamId += 64;
                offset++;
            } else if (header.chunkStreamId == 1) {
                header.chunkStreamId = decodeByte(data, offset + 1) << 8 + decodeByte(data, offset);
                header.chunkStreamId += 64;
                offset += 2;
            }

            int msgHeaderLen = MESSAGE_HEAD_LEN[header.fmt];
            if (msgHeaderLen > 0) {
                header.timestamp = decodeInt24(data, offset);
                offset += 3;
            }
            if (msgHeaderLen > 3) {
                header.bodySize = decodeInt24(data, offset);
                offset += 3;
                header.msgTypeId = data[offset++];
            }
            if (msgHeaderLen > 7) {
                header.msgStreamId = decodeInt32(data, offset);
                offset += 4;
            }
            if (header.timestamp == 0xffffff) {
                header.hasAbsTimestamp = true;
                header.timestamp = decodeInt32(data, offset);
                offset += 4;
            }
        } catch (RTMPDecoderException e) {
            e.printStackTrace();
            return -1;
        }
        int headerLen = offset;
        if (offset + header.bodySize > data.length) {
            return -1;
        } else {
            byte[] temp = new byte[data.length - offset];
            System.arraycopy(data, offset, temp, 0, data.length - offset);
            data = temp;
        }

        //Body
        int result = -1;
        int bodyLen = data.length - headerLen;
        if (header.msgTypeId == MSG_TYPE_PING) {
            try {
                body.eventType = decodeInt16(data, 0);
                body.eventStreamId = decodeInt32(data, 2);
                result = 6;
                if (body.eventType == 3) {
                    body.eventBufferLen = decodeInt32(data, 6);
                    result = 10;
                }
            } catch (RTMPDecoderException e) {
                e.printStackTrace();
                return -1;
            }
        } else if (header.msgTypeId == MSG_TYPE_SERVER_BW) {
            if (data.length < 4) {
                return -1;
            }
            try {
                body.WindowAcknowledgementSize = decodeInt32(data, 0);
            } catch (RTMPDecoderException e) {
                e.printStackTrace();
            }
            result = headerLen + 4;
        } else if (header.msgTypeId == MSG_TYPE_INVOKE) {
            int count = bodyLen / (AMFMaxSize + 1);
            int subSize = count;
            while (count > 0) {
                data = Util.deleteIndex(data, count * (AMFMaxSize + 1) - 1);
                count--;
            }

            result = AMF.decode(body, data, header.bodySize);
            if (result >= 0) {
                result = headerLen + result + subSize;
            } else {
                result = -1;
            }
        }

        return result == data.length ? 0 : result;
    }

    @Override
    public String toString() {
        return header.toString() + body.toString();
    }
}
