package cn.sskbskdrin.server.socket;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class Body {
	public int head;
	public int flag;
	public byte[] data;
	public int crc;

	@Override
	public String toString() {
		return "body(" + Integer.toHexString(head).toUpperCase() + Integer.toHexString(flag).toUpperCase() + " " + new String(data) + ";" + crc + ")";
	}
}
