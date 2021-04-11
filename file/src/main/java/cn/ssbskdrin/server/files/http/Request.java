package cn.ssbskdrin.server.files.http;

import java.util.HashMap;
import java.util.Map;

public class Request {
    String uri;
    String protocolVersion;
    Map<String, String> header = new HashMap<>();
    Map<String, Object> params = new HashMap<>();

    public Method getMethod() {
        return Method.lookup(header.get("method"));
    }

    public long getContentLength() {
        if (header.containsKey("content-length")) {
            return Long.parseLong(header.get("content-length"));
            //            } else if (this.splitbyte < this.rlen) {
            //                return this.rlen - this.splitbyte;
        }
        return 0;
    }
}