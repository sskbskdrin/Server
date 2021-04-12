package cn.ssbskdrin.server.files.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import cn.ssbskdrin.server.files.ByteBufferUtil;
import cn.ssbskdrin.server.files.Log;
import cn.ssbskdrin.server.files.core.ChannelContext;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class HttpHandler extends ChannelContext {
    private int readLen;
    private int headerLen = 0;
    Request request;

    Response response;
    SendResponse sendResponse = new SendResponse() {
        @Override
        public void send(Response rsp) {
            response = rsp;
            try {
                buffer.clear();
                response.write(buffer);
                buffer.flip();
                switchMode(false);
            } catch (ClosedChannelException e) {
                onException(e);
            }
        }
    };

    @Override
    protected boolean isWriteComplete() {
        buffer.compact();
        buffer.flip();
        return super.isWriteComplete();
    }

    @Override
    public void onReceive() throws Exception {
        byte[] bytes = buffer.array();
        int len = buffer.remaining();
        if (headerLen == 0) {
            int offset = readLen < 4 ? 0 : readLen - 4;
            headerLen = findHeaderEnd(bytes, offset, len - offset);
            readLen = len;
            if (headerLen == 0) return;
        }
        if (headerLen > 0 && request == null) {
            try {
                parseHeader(bytes, headerLen);
            } catch (ResponseException e) {
                sendResponse.send(Response.newFixedLengthResponse(e.getStatus().getDescription()));
            }
        }
        buffer.compact();
        buffer.flip();
        serve();
    }

    private static int findHeaderEnd(final byte[] buf, int offset, int len) {
        int index = offset;
        while (index + 1 < len) {

            // RFC2616
            if (buf[index] == '\r' && buf[index + 1] == '\n' && index + 3 < len && buf[index + 2] == '\r' && buf[index + 3] == '\n') {
                return index + 4;
            }

            // tolerance
            if (buf[index] == '\n' && buf[index + 1] == '\n') {
                return index + 2;
            }
            index++;
        }
        return 0;
    }

    private void parseHeader(byte[] bytes, int len) throws ResponseException {
        request = new Request();
        try {
            // Read the request line
            BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes, 0, len)));
            String inLine = in.readLine();
            if (inLine == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax " + "error. Usage: GET "
                    + "/example/file.html");
            }

            request.header.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new ResponseException(Response.Status.BAD_REQUEST,
                    "BAD REQUEST: Missing " + "URI. Usage: GET " + "/example/file.html");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParams(uri.substring(qmi + 1), request.params);
                uri = decodePercent(uri.substring(0, qmi));
            } else {
                uri = decodePercent(uri);
            }

            request.header.put("uri", uri);
            request.uri = uri;

            // If there's another token, its protocol version,
            // followed by HTTP headers.
            // NOTE: this now forces header names lower case since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                request.protocolVersion = st.nextToken();
            } else {
                request.protocolVersion = "HTTP/1.1";
                Log.w("no protocol version specified, strange. Assuming HTTP/1.1.");
            }
            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    request.header.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1)
                        .trim());
                }
                line = in.readLine();
            }

        } catch (IOException ioe) {
            throw new ResponseException(Response.Status.INTERNAL_ERROR,
                "SERVER INTERNAL ERROR: " + "IOException: " + ioe
                .getMessage(), ioe);
        }
    }

    static void decodeParams(String params, Map<String, Object> p) {
        if (params == null) return;
        StringTokenizer st = new StringTokenizer(params, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            String key;
            String value = "";

            if (sep >= 0) {
                key = decodePercent(e.substring(0, sep)).trim();
                value = decodePercent(e.substring(sep + 1));
            } else {
                key = decodePercent(e).trim();
            }

            Object values = p.get(key);
            if (values == null) {
                p.put(key, value);
            } else if (values instanceof String) {
                List<String> list = new ArrayList<>();
                list.add((String) values);
                list.add(value);
                p.put(key, list);
            } else {
                ((List<String>) values).add(value);
            }
        }
    }

    private void serve() {
        Method method = request.getMethod();
        switch (method) {
            case GET:
                HttpDispatch.get(request, sendResponse);
                break;
            case PUT:
            case POST:
                HttpDispatch.post(request, sendResponse, buffer);
                break;
            default:
                sendResponse.send(Response.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not " +
                    "Found"));
                break;
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        ByteBufferUtil.recycle(buffer);
    }

    private static String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decoded;
    }


}
