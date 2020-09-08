package cn.sskbskdrin.server.rtmp;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class RTMPChunk {
    public int headerSize;
    public int chunkSize;
    public byte[] header;
    public byte[] chunk;
}
