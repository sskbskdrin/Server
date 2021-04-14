package cn.ssbskdrin.server.files.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import cn.ssbskdrin.server.files.util.Log;

/**
 * HTTP response
 */
public class Response implements Closeable {
    private static final byte[] END = "\r\n".getBytes();

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DATE = "Date";
    private static final String CONNECTION = "Connection";
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String TRANSFER_ENCODING = "Transfer-Encoding";
    private static final String CONTENT_LENGTH = "Content-Length";

    /**
     * Some HTTP response status codes
     */
    public enum Status {
        SWITCH_PROTOCOL(101, "Switching Protocols"),

        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        NO_CONTENT(204, "No Content"),
        PARTIAL_CONTENT(206, "Partial Content"),
        MULTI_STATUS(207, "Multi-Status"),

        REDIRECT(301, "Moved Permanently"),
        /**
         * Many user agents mishandle 302 in ways that violate the RFC1945
         * spec (i.e., redirect a POST to a GET). 303 and 307 were added in
         * RFC2616 to address this. You should prefer 303 and 307 unless the
         * calling user agent does not support 303 and 307 functionality
         */
        @Deprecated FOUND(302, "Found"),
        REDIRECT_SEE_OTHER(303, "See Other"),
        NOT_MODIFIED(304, "Not Modified"),
        TEMPORARY_REDIRECT(307, "Temporary Redirect"),

        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        NOT_ACCEPTABLE(406, "Not " + "Acceptable"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        CONFLICT(409, "Conflict"),
        GONE(410, "Gone"),
        LENGTH_REQUIRED(411, "Length Required"),
        PRECONDITION_FAILED(412, "Precondition Failed"),
        PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
        EXPECTATION_FAILED(417, "Expectation " + "Failed"),
        TOO_MANY_REQUESTS(429, "Too Many Requests"),

        INTERNAL_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),
        UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not " + "Supported");

        private final int requestStatus;

        private final String description;

        Status(int requestStatus, String description) {
            this.requestStatus = requestStatus;
            this.description = description;
        }

        public static Status lookup(int requestStatus) {
            for (Status status : Status.values()) {
                if (status.getRequestStatus() == requestStatus) {
                    return status;
                }
            }
            return null;
        }

        public String getDescription() {
            return "" + this.requestStatus + " " + this.description;
        }

        public int getRequestStatus() {
            return this.requestStatus;
        }

    }

    /**
     * Output stream that will automatically send every write to the wrapped
     * OutputStream according to chunked transfer:
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6.1
     */
    private static class ChunkOutputStream extends FilterOutputStream {

        public ChunkOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            byte[] data = {(byte) b};
            write(data, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len == 0) return;
            out.write(String.format("%x\r\n", len).getBytes());
            out.write(b, off, len);
            out.write("\r\n".getBytes());
        }

    }

    /**
     * HTTP status code after processing, e.g. "200 OK", Status.OK
     */
    private Status status;

    /**
     * MIME type of content, e.g. "text/html"
     */
    private String mimeType;

    /**
     * Data of the response, may be null.
     */
    private InputStream data;

    private long contentLength;

    /**
     * Headers for the HTTP response. Use addHeader() to add lines. the
     * lowercase map is automatically kept up to date.
     */
    private Map<String, String> header;

    /**
     * The request method that spawned this response.
     */
    private Method requestMethod;

    /**
     * Creates a fixed length response if totalBytes>=0, otherwise chunked.
     */
    private Response(Builder builder) {
        this.status = builder.status;
        this.header = builder.header;
        if (builder.data == null) {
            builder.data = new ByteArrayInputStream(new byte[0]);
            builder.header(CONTENT_LENGTH, "0");
        }
        this.data = builder.data;
        this.requestMethod = builder.requestMethod;
    }

    @Override
    public void close() throws IOException {
        if (this.data != null) {
            this.data.close();
        }
    }

    public String getHeader(String name) {
        return this.header.get(name);
    }

    public Method getRequestMethod() {
        return this.requestMethod;
    }

    public Status getStatus() {
        return this.status;
    }

    private boolean hasRemaining;
    private boolean firstWrite = true;

    boolean hasRemaining() {
        return hasRemaining;
    }

    protected void write(ByteBuffer buffer) {
        if (firstWrite) {
            if (getHeader(DATE) == null) {
                SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                header.put(DATE, gmtFrmt.format(new Date()));
            }
            String contentEncoding = getHeader(CONTENT_ENCODING);
            if (contentEncoding != null && contentEncoding.contains("gzip")) {
                header.put(TRANSFER_ENCODING, "chunked");
            }
            if ("chunked".equals(getHeader(TRANSFER_ENCODING))) {
                this.header.remove(CONTENT_LENGTH);
            }
            if (this.status == null) {
                throw new Error("sendResponse(): Status can't be null.");
            }
            printHeader(buffer, "HTTP/1.1 " + this.status.getDescription() + " ");
            for (Map.Entry<String, String> entry : this.header.entrySet()) {
                printHeader(buffer, entry.getKey(), entry.getValue());
            }
            printHeader(buffer, "");
            firstWrite = false;
        }
        try {
            //
            //            long pending = this.data != null ? this.contentLength : 0;
            //            if (this.requestMethod != Method.HEAD && this.header.containsKey(TRANSFER_ENCODING)) {
            //                //                printHeader(channel, buffer, "Transfer-Encoding", "chunked");
            //            } else if (!encodeAsGzip) {
            //                pending = sendContentLengthHeaderIfNotAlreadyPresent(buffer, pending);
            //            }
            boolean gzip = "gzip".equals(getHeader(CONTENT_ENCODING));
            boolean chunk = "chunked".equals(getHeader(TRANSFER_ENCODING));
            byte[] cache = new byte[Math.min(buffer.remaining() * 2 / 3, data.available())];
            if (data.available() > 0) {
                int len = data.read(cache);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(len + 20);
                OutputStream os = bos;
                if (chunk) {
                    os = new ChunkOutputStream(os);
                }
                if (gzip) {
                    os = new GZIPOutputStream(os);
                }
                os.write(cache, 0, len);
                if (os instanceof GZIPOutputStream) {
                    ((GZIPOutputStream) os).finish();
                }
                buffer.put(bos.toByteArray());
            }
            if (data.available() <= 0) {
                if (chunk) {
                    if (buffer.remaining() > 4) {
                        buffer.put("0\r\n\r\n".getBytes());
                        hasRemaining = false;
                    } else {
                        hasRemaining = true;
                    }
                } else {
                    buffer.put(END);
                    hasRemaining = false;
                }
            } else {
                hasRemaining = true;
            }
        } catch (IOException ioe) {
            Log.w("Could not send response to the client", ioe);
        }
    }

    private static void printHeader(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes());
        buffer.put(END);
    }

    private static void printHeader(ByteBuffer buffer, String key, String value) {
        buffer.put(key.getBytes());
        buffer.put((byte) ':');
        buffer.put((byte) ' ');
        buffer.put(value.getBytes());
        buffer.put(END);
    }

    /**
     * Create a response with known length.
     */
    private static Response newFixedLengthResponse(Response.Status status, String mimeType, InputStream data,
                                                   long totalBytes) {
        return Response.with(status).contentType(mimeType).body(data).contentLength(totalBytes).build();
    }

    /**
     * Create a text response with known length.
     */
    private static Response newFixedLengthResponse(Response.Status status, String mimeType, String txt) {
        ContentType contentType = new ContentType(mimeType);
        if (txt == null) {
            return newFixedLengthResponse(status, mimeType, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(txt)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = txt.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                Log.w("encoding problem, responding nothing", e);
                bytes = new byte[0];
            }
            return newFixedLengthResponse(status, contentType.getContentTypeHeader(), new ByteArrayInputStream(bytes)
                , bytes.length);
        }
    }

    public static Builder with(Status status) {
        return new Builder(status);
    }

    public static class Builder {

        /**
         * HTTP status code after processing, e.g. "200 OK", Status.OK
         */
        private final Status status;

        /**
         * Data of the response, may be null.
         */
        private InputStream data;

        /**
         * Headers for the HTTP response. Use addHeader() to add lines. the
         * lowercase map is automatically kept up to date.
         */
        private final Map<String, String> header = new HashMap<>();

        /**
         * The request method that spawned this response.
         */
        private Method requestMethod;

        private Builder(Status status) {
            this.status = status;
            contentType("text/plain");
            keepAlive(true);
        }

        public Builder contentType(String mimeType) {
            return header(CONTENT_TYPE, mimeType);
        }

        public Builder contentLength(long len) {
            return header(CONTENT_LENGTH, String.valueOf(len));
        }

        public Builder enableGzip(boolean enable) {
            return header(CONTENT_ENCODING, enable ? "gzip" : null);
        }

        public Builder enableTransfer(boolean enable) {
            return header(TRANSFER_ENCODING, enable ? "chunked" : null);
        }

        public Builder requestMethod(Method method) {
            this.requestMethod = method;
            return this;
        }

        public Builder keepAlive(boolean keep) {
            return header(CONNECTION, keep ? "Keep-Alive" : null);
        }

        public Builder closeConnection(boolean close) {
            return header(CONNECTION, close ? "close" : null);
        }

        public Builder header(String key, String value) {
            if (key == null || key.length() == 0 || value == null || value.length() == 0) {
                header.remove(CONTENT_TYPE);
            } else {
                header.put(key, value);
            }
            return this;
        }

        public Builder body(String body) {
            byte[] bytes;
            if (body == null) {
                bytes = new byte[0];
            } else {
                try {
                    ContentType contentType = new ContentType(header.get(CONTENT_TYPE));
                    CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                    if (!newEncoder.canEncode(body)) {
                        contentType = contentType.tryUTF8();
                    }
                    bytes = body.getBytes(contentType.getEncoding());
                } catch (UnsupportedEncodingException e) {
                    Log.w("encoding problem, responding nothing", e);
                    bytes = new byte[0];
                }
            }
            return body(new ByteArrayInputStream(bytes));
        }

        public Builder body(InputStream is) {
            try {
                contentLength(is.available());
            } catch (IOException e) {
                e.printStackTrace();
            }
            data = is;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }

}
