package cn.sskbskdrin.server.rtmp;

import java.util.ArrayList;

import static cn.sskbskdrin.server.rtmp.Util.decodeBoolean;
import static cn.sskbskdrin.server.rtmp.Util.decodeByte;
import static cn.sskbskdrin.server.rtmp.Util.decodeInt24;
import static cn.sskbskdrin.server.rtmp.Util.decodeNumber;
import static cn.sskbskdrin.server.rtmp.Util.decodeString;
import static cn.sskbskdrin.server.rtmp.Util.encodeObjectProperty;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class AMF {

    enum AMFDataType {
        AMF_NUMBER(0), AMF_BOOLEAN(1), AMF_STRING(2), AMF_OBJECT(3), AMF_MOVIECLIP(4),        /* reserved(), not used */
        AMF_NULL(5), AMF_UNDEFINED(6), AMF_REFERENCE(7), AMF_ECMA_ARRAY(8), AMF_OBJECT_END(9), AMF_STRICT_ARRAY(0x0a)
        , AMF_DATE(0x0b), AMF_LONG_STRING(0x0c), AMF_UNSUPPORTED(0x0d), AMF_RECORDSET(0x0e), AMF_XML_DOC(0x0f),
        AMF_TYPED_OBJECT(0x10), AMF_AVMPLUS(0x11),        /* switch to AMF3 */
        AMF_INVALID(0x12);

        byte value;

        AMFDataType(int v) {
            value = (byte) v;
        }

        static AMFDataType getType(int type) {
            for (AMFDataType t : AMFDataType.values()) {
                if (t.value == type) {
                    return t;
                }
            }
            return AMF_NULL;
        }
    }

    public static class AMFObject {
        ArrayList<AMFObjectProperty> list = new ArrayList<>();

        public void addProperty(String name, boolean value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.name = name;
            property.type = AMFDataType.AMF_BOOLEAN;
            property.bValue = value;
            addProperty(property);
        }

        public void addProperty(String name, String value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.name = name;
            property.type = AMFDataType.AMF_STRING;
            property.sValue = value;
            addProperty(property);
        }

        public void addProperty(String name, double value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.name = name;
            property.type = AMF.AMFDataType.AMF_NUMBER;
            property.nValue = value;
            addProperty(property);
        }

        public void addProperty(String name, AMF.AMFObject value) {
            AMF.AMFObjectProperty property = new AMF.AMFObjectProperty();
            property.name = name;
            property.type = AMFDataType.AMF_OBJECT;
            property.oValue = value;
            addProperty(property);
        }

        public void addProperty(AMF.AMFObjectProperty property) {
            list.add(property);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (AMFObjectProperty p : list) {
                builder.append("\t");
                builder.append(p.toString());
                builder.append('\n');
            }
            if (builder.length() > 0) {
                builder.setLength(builder.length() - 1);
            } else {
                builder.append("AMFObject list is empty");
            }
            return builder.toString();
        }
    }

    static class AMFObjectProperty {
        String name;//嵌套类型时才有name

        AMFDataType type;
        double nValue;
        String sValue;
        boolean bValue;
        AMFObject oValue;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (name != null) {
                builder.append('\'');
                builder.append(name);
                builder.append('\'');
                builder.append(' ');
            }
            switch (type) {
                case AMF_NUMBER:
                    builder.append("Number ");
                    builder.append(nValue);
                    break;
                case AMF_BOOLEAN:
                    builder.append("Boolean ");
                    builder.append(bValue);
                    break;
                case AMF_STRING:
                    builder.append("String \'");
                    builder.append(sValue);
                    builder.append('\'');
                    break;
                case AMF_OBJECT:
                    builder.append("Object \n");
                    builder.append(oValue.toString());
                    break;
                case AMF_NULL:
                    builder.append("Null(0x05)");
                    break;
                default:
            }
            return builder.toString();
        }
    }

    static void encode(RTMPPacket.Body body) {
        body.data = new ByteList();
        for (AMFObjectProperty property : body.amfObject.list) {
            encodeObjectProperty(body.data, property);
        }
    }

    static int decode(RTMPPacket.Body body, byte[] data, int bodySize) {
        if (body.amfObject == null) {
            body.amfObject = new AMFObject();
        }
        try {
            int offset = 0;
            while (offset < bodySize) {
                AMFObjectProperty property = new AMFObjectProperty();
                offset += decodeProperty(property, data, offset, false);
                body.amfObject.addProperty(property);
            }
            return offset;
        } catch (RTMPDecoderException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * @return 返回解码消耗掉的长度, 小于0表示解码失败
     */
    private static int decode(AMFObject object, byte[] data, int offset, boolean decodeName) throws RTMPDecoderException {
        final int nOffset = offset;
        while (offset < data.length) {
            if (decodeInt24(data, offset) == AMFDataType.AMF_OBJECT_END.value) {
                offset += 3;
                break;
            }
            AMFObjectProperty property = new AMFObjectProperty();
            offset += decodeProperty(property, data, offset, decodeName);
            object.list.add(property);
        }
        return offset - nOffset;
    }

    private static int decodeProperty(AMFObjectProperty property, byte[] data, int offset, boolean decodeName) throws RTMPDecoderException {
        final int nOffset = offset;
        if (decodeName) {
            property.name = decodeString(data, offset);
            offset += property.name.length() + 2;
        }
        property.type = AMFDataType.getType(decodeByte(data, offset++));
        switch (property.type) {
            case AMF_NUMBER:
                property.nValue = decodeNumber(data, offset);
                offset += 8;
                break;
            case AMF_BOOLEAN:
                property.bValue = decodeBoolean(data, offset);
                offset++;
                break;
            case AMF_STRING:
                property.sValue = decodeString(data, offset);
                offset += property.sValue.length() + 2;
                break;
            case AMF_OBJECT:
                property.oValue = new AMFObject();
                offset += decode(property.oValue, data, offset, true);
                break;
            default:
                break;
        }
        return offset - nOffset;
    }

    public static void main(String[] args) {
        String data =
            "03 00 00 00 00 00 ca 14 00 00 00 00 02 00 07 63 6f 6e 6e 65 63 74 00 3f f0 00 00 00 00 00 00 " + "03" +
                " 00 03 61 70 70 02 00 05 6c 69 76 65 74 00 08 66 6c 61 73 68 56 65 72 02 00 0d 4c 4e 58 20 39 2c " + "30 2c " + "31 32 34 2c 32 00 05 74 63 55 72 6c 02 00 1f 72 74 6d 70 3a 2f 2f 31 39 32 2e 31 36 38 2e 30 " + "2e 31 30 31" + " 3a 38 30 39 30 2f 6c 69 76 65 74 00 04 66 70 61 64 01 00 00 0c 63 61 70 61 62 69 6c 69 " + "74 69 65 73 00 40 2e 00 00 00 c3 00 00 00 00 0b 61 75 64 69 6f 43 6f 64 65 63 73 00 40 af ce 00 00 00 00" + " 00 00 0b 76 69 64 65 6f 43 6f 64 65 63 73 00 40 6f 80 00 00 00 00 00 00 0d 76 69 64 65 6f 46 75 6e 63 " + "74 69 6f 6e 00 3f f0 00 00 00 00 00 00 00 00 09 ";
        byte[] bytes = new byte[data.length() / 3];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(data.substring(i * 3, i * 3 + 2), 16);
        }
        RTMPPacket packet = new RTMPPacket();
        //        packet.decode(bytes,null);
        //        System.out.println(packet.toString());


        byte[] test = new byte[]{0, 1, 2, 3, 4, 0, 5, 6, 7, 0, 8, 9, 10, 0, 11, 12, 13, 0};
        int headerLen = 2;
        int bodyLen = test.length - headerLen;
        int AMFMaxSize = 3;
        int count = bodyLen / (AMFMaxSize + 1);
        while (count > 0) {
            test = Util.deleteIndex(test, headerLen + count * (AMFMaxSize + 1) - 1);
            count--;
        }
        //        Util.printBytes(test);
    }
}
