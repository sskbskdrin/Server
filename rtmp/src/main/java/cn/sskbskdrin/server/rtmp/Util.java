package cn.sskbskdrin.server.rtmp;

import cn.sskbskdrin.server.util.SLog;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class Util {
    private static final String TAG = "Util";

    public static boolean decodeBoolean(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 1);
        return data[offset] != 0;
    }

    public static int decodeByte(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 1);
        return data[offset] & 0xff;
    }

    public static int decodeInt16(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 2);
        int value = 0;
        value |= (data[offset++] & 0xff) << 8;
        value |= (data[offset] & 0xff);
        return value;
    }

    public static int decodeInt24(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 3);
        int value = 0;
        value |= (data[offset++] & 0xff) << 16;
        value |= (data[offset++] & 0xff) << 8;
        value |= (data[offset] & 0xff);
        return value;
    }

    public static int decodeInt32(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 4);
        int value = 0;
        value |= (data[offset++] & 0xff) << 24;
        value |= (data[offset++] & 0xff) << 16;
        value |= (data[offset++] & 0xff) << 8;
        value |= (data[offset] & 0xff);
        return value;
    }

    public static double decodeNumber(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 8);
        long value = 0;
        value |= (data[offset++] & 0xffL) << 56;
        value |= (data[offset++] & 0xffL) << 48;
        value |= (data[offset++] & 0xffL) << 40;
        value |= (data[offset++] & 0xffL) << 32;
        value |= (data[offset++] & 0xffL) << 24;
        value |= (data[offset++] & 0xffL) << 16;
        value |= (data[offset++] & 0xffL) << 8;
        value |= (data[offset] & 0xffL);
        return Double.longBitsToDouble(value);
    }

    public static String decodeString(byte[] data, int offset) throws RTMPDecoderException {
        checkArrayAndOffsetSize(data, offset, 2);
        int len = decodeInt16(data, offset);
        checkArrayAndOffsetSize(data, offset, len);
        return new String(data, offset + 2, len);
    }

    static void checkArrayAndOffsetSize(byte[] data, int offset, int size) throws RTMPDecoderException {
        if (data == null) {
            throw new RTMPDecoderException("data is null");
        }
        if (offset + size > data.length) {
            throw new RTMPDecoderException("Array index out of range data.len=" + data.length + " off=" + offset + " "
                + "size=" + size);
        }
    }

    static void encodeInt16(ByteList data, int value) {
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    static void encodeInt24(ByteList data, int value) {
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    static void encodeInt32(ByteList data, int value) {
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    static void encodeIntB(ByteList data, int value) {
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    static void encodeIntL(ByteList data, int value) {
        data.add((byte) value);
        data.add((byte) (value >> 8));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 24));
    }

    static void encodeBoolean(ByteList data, boolean value) {
        data.add(AMF.AMFDataType.AMF_BOOLEAN.value);
        data.add((byte) (value ? 1 : 0));
    }

    static void encodeNumber(ByteList data, double d) {
        data.add(AMF.AMFDataType.AMF_NUMBER.value);
        long value = Double.doubleToRawLongBits(d);
        data.add((byte) (value >> 56));
        data.add((byte) (value >> 48));
        data.add((byte) (value >> 40));
        data.add((byte) (value >> 32));
        data.add((byte) (value >> 24));
        data.add((byte) (value >> 16));
        data.add((byte) (value >> 8));
        data.add((byte) value);
    }

    static void encodeString(ByteList data, String value) {
        data.add(AMF.AMFDataType.AMF_STRING.value);
        encodeInt16(data, value.length());
        data.addAll(value.getBytes());
    }

    static void encodeNameString(ByteList data, String name, String value) {
        if (name != null) {
            encodeInt16(data, name.length());
            data.addAll(name.getBytes());
        }
        encodeString(data, value);
    }

    static void encodeNameNumber(ByteList data, String name, double value) {
        if (name != null) {
            encodeInt16(data, name.length());
            data.addAll(name.getBytes());
        }
        encodeNumber(data, value);
    }

    static void encodeNameBoolean(ByteList data, String name, boolean value) {
        if (name != null) {
            encodeInt16(data, name.length());
            data.addAll(name.getBytes());
        }
        encodeBoolean(data, value);
    }

    static void encodeObject(ByteList data, AMF.AMFObject value) {
        data.add(AMF.AMFDataType.AMF_OBJECT.value);
        for (AMF.AMFObjectProperty property : value.list) {
            encodeObjectProperty(data, property);
        }
        encodeInt24(data, AMF.AMFDataType.AMF_OBJECT_END.value);
    }

    static void encodeNameObject(ByteList data, String name, AMF.AMFObject value) {
        if (name != null) {
            encodeInt16(data, name.length());
            data.addAll(name.getBytes());
        }
        encodeObject(data, value);
    }

    static void encodeObjectProperty(ByteList data, AMF.AMFObjectProperty property) {
        switch (property.type) {
            case AMF_NUMBER:
                encodeNameNumber(data, property.name, property.nValue);
                break;
            case AMF_BOOLEAN:
                encodeNameBoolean(data, property.name, property.bValue);
                break;
            case AMF_STRING:
                encodeNameString(data, property.name, property.sValue);
                break;
            case AMF_OBJECT:
                encodeNameObject(data, property.name, property.oValue);
                break;
            case AMF_NULL:
                data.add(property.type.value);
                break;
            default:
        }
    }

    static byte[] deleteIndex(byte[] data, int index) {
        if (index < data.length) {
            byte[] temp = new byte[data.length - 1];
            System.arraycopy(data, 0, temp, 0, index);
            System.arraycopy(data, index + 1, temp, index, data.length - index - 1);
            data = temp;
        }
        return data;
    }

    public static void printBytes(byte[] data) {
        if (data != null) {
            SLog.d(TAG, "data len=" + data.length);
            StringBuilder builder = new StringBuilder("byte=");
            for (byte b : data) {
                String v = Integer.toHexString(b & 0xff);
                if (v.length() == 1) {
                    builder.append('0');
                }
                builder.append(v);
                builder.append(' ');
            }
            SLog.i(TAG, builder.toString());
        } else {
            SLog.i(TAG, "byte=null");
        }
    }
}
