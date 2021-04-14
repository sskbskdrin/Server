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

    private long contentLength = -1;

    public long getContentLength() {
        if (contentLength < 0) {
            String len = header.get("content-length");
            try {
                contentLength = Long.parseLong(len == null ? "0" : len);
            } catch (NumberFormatException ignored) {
            }
            contentLength = 0;
        }
        return contentLength;
    }

    private ContentType contentType;

    public ContentType getContentType() {
        if (contentType == null) {
            contentType = new ContentType(header.get("content-type"));
        }
        return contentType;
    }
}