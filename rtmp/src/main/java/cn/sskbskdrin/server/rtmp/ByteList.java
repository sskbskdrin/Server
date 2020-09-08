package cn.sskbskdrin.server.rtmp;

import java.util.Arrays;

/**
 * @author sskbskdrin
 * @date 2019/April/28
 */
public class ByteList {

    private byte[] value = new byte[10];
    private int size = 0;

    public ByteList() {
    }

    public ByteList(int capacity) {
        value = new byte[capacity];
    }

    public ByteList(byte[] data) {
        value = data;
        size = data.length;
    }

    public void add(byte b) {
        ensureCapacity(size + 1);
        value[size++] = b;
    }

    public void addAll(byte[] data) {
        ensureCapacity(size + data.length);
        System.arraycopy(data, 0, value, size, data.length);
        size += data.length;
    }

    private void ensureCapacity(int minLen) {
        int old = value.length;
        if (minLen <= old) {
            return;
        }

        int newLen = old + old >> 1;
        if (newLen < minLen) {
            newLen = minLen;
        }
        value = Arrays.copyOf(value, newLen);
    }

    public byte[] getValue() {
        return value;
    }

    public int size() {
        return size;
    }
}
