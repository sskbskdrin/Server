package cn.ssbskdrin.server.files.http;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class HttpServlet implements Closeable {

    private static final long MEMORY_STORE_LIMIT = 1024 * 512;

    private long currLen = 0;
    RandomAccessFile tmpFile;
    DataOutput output;

    protected Response get(Request request) {
        try {
            return Response.with(Response.Status.OK)
                //                .enableTransfer(true)
                .header("Content-Disposition", "attachment;filename=" + "opencv-4.5.1-android-sdk.zip")
                .body(new FileInputStream("C:\\Users\\keayuan\\Downloads\\opencv-4.5.1-android-sdk.zip"))
                .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.with(Response.Status.OK).body("get").build();
    }

    protected Response post(Request request, ByteBuffer buffer) {
        try {
            if (parseBody(request, buffer)) {
                return post(request);
            }
        } catch (IOException ioe) {
            return Response.with(Response.Status.INTERNAL_ERROR)
                .contentType("text/plain")
                .body("SERVER INTERNAL ERROR: IOException: " + ioe.getMessage())
                .build();
        } catch (ResponseException re) {
            try {
                tmpFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Response.with(Response.Status.lookup(re.getStatus().getRequestStatus()))
                .contentType("text/plain")
                .body(re.getMessage())
                .build();
        }
        return null;
    }

    protected Response post(Request request) {
        Response rsp = Response.with(Response.Status.OK).enableGzip(true).body("this is test post gzip").build();
        return rsp;
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
            return new RandomAccessFile(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + "/body" +
                ".tmp", "rw");
        } catch (Exception e) {
            throw new Error(e); // we won't recover, so throw an error
        }
    }

    private List<Integer> boundaryPosition;
    private byte[] boundaryCache;

    private boolean parseBody(Request request, ByteBuffer buffer) throws IOException, ResponseException {
        RandomAccessFile randomAccessFile = null;
        long totalLength = getBodySize(request);
        if (totalLength == 0) {
            totalLength = buffer.remaining();
        }
        ContentType contentType = new ContentType(request.header.get("content-type"));
        if (contentType.isMultipart()) {
            String boundary = contentType.getBoundary();
            boundaryCache = new byte[boundary.length() * 2];
        }

        currLen += buffer.remaining();
        if (output != null) {
            output.write(buffer.array(), buffer.position(), buffer.remaining());
            buffer.position(buffer.limit());
        } else if (totalLength > buffer.capacity()) {
            // Store the request in memory or a file, depending on size
            if (totalLength < MEMORY_STORE_LIMIT) {
                output = new DataOutputStream(new ByteArrayOutputStream());
            } else {
                output = getTmpBucket();
            }
            output.write(buffer.array(), buffer.position(), buffer.remaining());
            buffer.position(buffer.limit());
        }
        if (output == null) {
            if (buffer.remaining() < totalLength) return false;
        } else {
            if (currLen < totalLength) return false;
        }
        return parseBody(request, output == null ? null : buffer.array(), buffer.position(), buffer.remaining());
    }

    private boolean parseBody(Request request, byte[] bytes, int offset, int len) throws ResponseException {
        ContentType contentType = new ContentType(request.header.get("content-type"));
        if (contentType.isMultipart()) {
            String boundary = contentType.getBoundary();
            if (boundary == null) {
                throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is " + "multipart"
                    + "/form-data but boundary missing.");
            }
            //            decodeMultipartFormData(contentType, fbuf, this.parms, files);
        } else if (bytes == null) {
            throw new ResponseException(Response.Status.PAYLOAD_TOO_LARGE, "body too large");
        } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType.getContentType())) {
            String postLine = new String(bytes, offset, len).trim();
            HttpHandler.decodeParams(postLine, request.params);
        } else if (len > 0) {
            request.params.put("postData", Arrays.copyOf(bytes, len));
        }
        return true;
    }

    private void findBoundary(byte[] bytes, int offset, int len, byte[] dest) {
        for (int i = 0; i < bytes.length; i++) {

        }
    }

    @Override
    public void close() throws IOException {

    }
}
