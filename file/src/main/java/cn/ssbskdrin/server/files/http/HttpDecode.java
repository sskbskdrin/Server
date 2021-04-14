package cn.ssbskdrin.server.files.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.ssbskdrin.server.files.Log;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class HttpDecode implements Closeable {

    private static final long MEMORY_STORE_LIMIT = 1024 * 512;

    private int readLen;
    private int headerLen = 0;
    Request request;

    protected boolean input(ByteBuffer buffer) throws ResponseException {
        byte[] bytes = buffer.array();
        int len = buffer.remaining();
        if (headerLen == 0) {
            int offset = readLen < 4 ? 0 : readLen - 4;
            headerLen = findHeaderEnd(bytes, offset, len - offset);
            readLen = len;
            if (headerLen == 0) return false;
        }
        if (headerLen > 0 && request == null) {
            parseHeader(bytes, headerLen);
            buffer.position(headerLen);
        }
        Method method = request.getMethod();
        if (method == Method.GET) {
            return true;
        } else if (method == Method.POST) {
            try {
                return parseBody(request, buffer);
            } catch (IOException e) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, e.getMessage());
            }
        } else {
            throw new ResponseException(Response.Status.NOT_FOUND, "not support method " + method.name());
        }
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
            if (Log.isLoggerAble(Log.INFO)) {
                Log.i(new String(bytes, 0, len));
            }
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

    private static String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decoded;
    }

    private ByteBuff output;

    private static class ByteBuff extends ByteArrayOutputStream {
        byte[] array() {
            return buf;
        }
    }

    private KMP kmpBoundary;
    private Part currPart;

    private boolean parseBody(Request request, ByteBuffer buffer) throws IOException, ResponseException {
        ContentType contentType = request.getContentType();
        if (contentType.isMultipart()) {
            return decodeMultipartFormData(request, buffer);
        }
        long totalLength = request.getContentLength();
        if (totalLength == 0) {
            totalLength = buffer.remaining();
        }
        if (totalLength > MEMORY_STORE_LIMIT)
            throw new ResponseException(Response.Status.PAYLOAD_TOO_LARGE, "body too large");

        if (totalLength > buffer.capacity()) {
            if (output == null) {
                output = new ByteBuff();
            }
        }
        if (output != null) {
            output.write(buffer.array(), buffer.position(), buffer.remaining());
            buffer.position(buffer.limit());
        }
        if (output == null) {
            if (buffer.remaining() < totalLength) return false;
        } else {
            if (output.size() < totalLength) return false;
        }
        int offset = output == null ? buffer.position() : 0;
        int len = output == null ? buffer.remaining() : output.size();
        return parseBody(request, output == null ? buffer.array() : output.array(), offset, len);
    }

    public boolean decodeMultipartFormData(Request request, ByteBuffer buffer) throws ResponseException, IOException {
        byte[] bytes = buffer.array();
        String boundary = request.getContentType().getBoundary();
        if (kmpBoundary == null) {
            kmpBoundary = new KMP(("--" + boundary).getBytes());
        }
        int pos;
        parsePartData(request, buffer);
        while ((pos = KMP.TWO_END.find(bytes, buffer.position(), buffer.remaining())) >= 0) {
            Part temp = parsePartHeader(request, bytes, buffer.position(), pos - buffer.position());
            currPart = (Part) request.params.get(temp.partName);
            if (currPart == null) {
                currPart = temp;
            } else {
                currPart.tempFileName = temp.tempFileName;
                currPart.partContentType = temp.partContentType;
            }
            buffer.position(pos + 4);
            parsePartData(request, buffer);
        }
        return new KMP((boundary + "--").getBytes()).find(buffer.array(), buffer.position(), buffer.remaining()) >= 0;
    }

    private Part parsePartHeader(Request request, byte[] bytes, int offset, int len) throws ResponseException {
        ContentType contentType = request.getContentType();
        BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes, offset, len),
            Charset
            .forName(contentType.getEncoding())), len);
        try {
            String line = in.readLine();
            if (line == null || !line.contains(contentType.getBoundary())) {
                throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is " + "multipart"
                    + "/form-data but chunk does not start with boundary.");
            }
            Part part = new Part();
            line = in.readLine();
            while (line != null) {
                Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String attributeString = matcher.group(2);
                    matcher = CONTENT_DISPOSITION_ATTRIBUTE_PATTERN.matcher(attributeString);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        if ("name".equalsIgnoreCase(key)) {
                            part.partName = matcher.group(2);
                        } else if ("filename".equalsIgnoreCase(key)) {
                            part.tempFileName = matcher.group(2);
                            // add these two line to support multiple
                            // files uploaded using the same field Id
                            if (!part.tempFileName.isEmpty()) {
                                // TODO
                                //                                if (pCount > 0) partName = partName + pCount++;
                                //                                else pCount++;
                            }
                        }
                    }
                }
                matcher = CONTENT_TYPE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    part.partContentType = matcher.group(2).trim();
                }
                line = in.readLine();
            }
            return part;
        } catch (IOException e) {
            throw new ResponseException(Response.Status.BAD_REQUEST, e.getMessage() + "BAD REQUEST: " +
                "parsePartHeader " + "boundary.");
        }
    }

    private void parsePartData(Request request, ByteBuffer buffer) throws IOException {
        if (currPart != null) {
            byte[] bytes = buffer.array();
            int pos = kmpBoundary.find(buffer.array(), buffer.position(), buffer.remaining());
            if (pos >= 0) {
                currPart.write(bytes, buffer.position(), pos - buffer.position() - 2);
                request.params.put(currPart.partName, currPart);
                buffer.position(pos);
                currPart.complete();
                currPart = null;
            } else {
                currPart.write(bytes, buffer.position(), buffer.remaining() - kmpBoundary.destLength());
                buffer.position(buffer.limit() - kmpBoundary.destLength());
            }
        }
    }

    private boolean parseBody(Request request, byte[] bytes, int offset, int len) throws ResponseException {
        ContentType contentType = request.getContentType();
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType.getContentType())) {
            String postLine = new String(bytes, offset, len).trim();
            decodeParams(postLine, request.params);
        } else if (len > 0) {
            request.params.put("postData", Arrays.copyOfRange(bytes, offset, offset + len));
        }
        return true;
    }

    private static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";

    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX,
        Pattern.CASE_INSENSITIVE);

    private static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";

    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]" +
        "([^\"^']*)['|\"]";

    private static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN =
        Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

    @Override
    public void close() throws IOException {

    }

    public static class Part {
        String partName = null;
        Set<String> fileName = null;
        String tempFileName = null;
        String partContentType = null;
        private byte[] data;
        ByteArrayOutputStream bos;
        DataOutput output;

        void write(byte[] bytes, int offset, int len) throws IOException {
            if (output == null) {
                if (tempFileName == null || tempFileName.length() == 0) {
                    bos = new ByteArrayOutputStream();
                    output = new DataOutputStream(bos);
                } else {
                    output = getTmpBucket();
                }
            }
            output.write(bytes, offset, len);
        }

        private RandomAccessFile getTmpBucket() {
            try {
                String name = System.getProperty("java.io.tmpdir") + "/NioServer/" + tempFileName;
                File file = new File(name);
                if (!file.exists()) {
                    file.createNewFile();
                }
                return new RandomAccessFile(file, "rw");
            } catch (Exception e) {
                throw new Error(e); // we won't recover, so throw an error
            }
        }

        private void complete() throws IOException {
            if (output instanceof DataOutputStream) {
                data = bos.toByteArray();
                bos = null;
            } else if (output instanceof RandomAccessFile) {
                try {
                    ((RandomAccessFile) output).close();
                    if (fileName == null) {
                        fileName = new HashSet<>();
                    }
                    fileName.add(tempFileName);
                } catch (IOException e) {
                    new File(System.getProperty("java.io.tmpdir") + "/NioServer/" + tempFileName).delete();
                    output = null;
                    throw e;
                }
            }
            output = null;
        }

        @Override
        public String toString() {
            return "Part{" + "partName='" + partName + '\'' + ", fileName=" + fileName + ", tempFileName='" + tempFileName + '\'' + ", partContentType='" + partContentType + '\'' + ", data=" + (data == null ? "" : new String(data)) + ", output=" + output + '}';
        }
    }
}
