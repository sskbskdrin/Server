package cn.sskbskdrin.server.rtmp;

/**
 * @author sskbskdrin
 * @date 2019/April/26
 */
public class RTMPHandshakeSimple {
    int timestampC1;
    int timestampS1;

    boolean c1 = false;
    boolean c2 = false;
    boolean s2 = false;
    byte C0 = 0;
    byte[] C1 = new byte[1536];

}
