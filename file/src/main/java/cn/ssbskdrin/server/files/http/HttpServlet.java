package cn.ssbskdrin.server.files.http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class HttpServlet {

    private static final long MEMORY_STORE_LIMIT = 1024 * 512;

    private long currLen = 0;
    RandomAccessFile tmpFile;
    DataOutput output;

    protected Response get(Request request) {
        return Response.newFixedLengthResponse("get test success");
    }

    protected void post(Request request, SendResponse sendResponse, ByteBuffer buffer) {
        try {
            parseBody(request, buffer);
        } catch (IOException ioe) {
            Response.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "SERVER " + "INTERNAL " +
                "ERROR: IOException: " + ioe
                .getMessage());
        } catch (ResponseException re) {
            Response.newFixedLengthResponse(Response.Status.lookup(re.getStatus()
                .getRequestStatus()), "text/plain", re.getMessage());
        }
    }

    private long getBodySize(Request request) {
        String len = request.header.get("content-length");
        try {
            return Long.parseLong(len == null ? "0" : len);
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private RandomAccessFile getTmpBucket() {
        try {
            return new RandomAccessFile(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + "body.tmp"
                , "rw");
        } catch (Exception e) {
            throw new Error(e); // we won't recover, so throw an error
        }
    }

    private void parseBody(Request request, ByteBuffer buffer) throws IOException, ResponseException {
        RandomAccessFile randomAccessFile = null;
        long totalLength = getBodySize(request);
        if (totalLength == 0) {
            totalLength = buffer.remaining();
        }
        currLen += buffer.remaining();
        if (output != null) {
            output.write(buffer.array(), buffer.position(), buffer.remaining());
        } else if (totalLength > buffer.capacity()) {
            // Store the request in memory or a file, depending on size
            if (totalLength < MEMORY_STORE_LIMIT) {
                output = new DataOutputStream(new ByteArrayOutputStream());
            } else {
                output = getTmpBucket();
            }
            output.write(buffer.array(), buffer.position(), buffer.remaining());
        }
        if (currLen < buffer.remaining()) {
            return;
        }
        if (output == null) {
            parseBody(request, buffer.array(), buffer.position(), buffer.remaining());
        } else {
            parseBody(request);
        }
    }

    private void parseBody(Request request, byte[] bytes, int offset, int len) throws ResponseException {
        ContentType contentType = new ContentType(request.header.get("content-type"));
        if (contentType.isMultipart()) {
            String boundary = contentType.getBoundary();
            if (boundary == null) {
                throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is " + "multipart"
                    + "/form-data but boundary missing.");
            }
            //            decodeMultipartFormData(contentType, fbuf, this.parms, files);
        } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType.getContentType())) {
            String postLine = new String(bytes, offset, len).trim();
            HttpHandler.decodeParams(postLine, request.params);
        } else if (len > 0) {
            request.params.put("postData", Arrays.copyOf(bytes, len));
        }
    }

    private void parseBody(Request request) throws ResponseException, IOException {
        ContentType contentType = new ContentType(request.header.get("content-type"));
        if (contentType.isMultipart()) {
            String boundary = contentType.getBoundary();
            if (boundary == null) {
                throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is " + "multipart"
                    + "/form-data but boundary missing.");
            }
            //            decodeMultipartFormData(contentType, fbuf, this.parms, files);
        } else {
            throw new ResponseException(Response.Status.PAYLOAD_TOO_LARGE, "body too large");
        }
    }

}
