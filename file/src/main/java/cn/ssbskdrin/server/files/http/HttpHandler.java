package cn.ssbskdrin.server.files.http;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.logging.Level;

import cn.ssbskdrin.server.files.ByteBufferUtil;
import cn.ssbskdrin.server.files.NanoHTTPD;
import cn.ssbskdrin.server.files.core.ChannelContext;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class HttpHandler extends ChannelContext {
    private Response response;
    private HttpDecode decode;
    private HttpServlet servlet = new HttpServlet() {
        @Override
        public Response get(Request request) {
            try {
                return Response.with(Response.Status.OK)
                    .header("Content-Disposition", "attachment;filename=" + "opencv-4.5.1-android-sdk.zip")
                    .body(new FileInputStream("C:\\Users\\keayuan\\Downloads\\opencv-4.5.1-android-sdk.zip"))
                    .build();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Response.with(Response.Status.OK).body("get").build();
        }

        @Override
        public Response post(Request request) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : request.params.entrySet()) {
                builder.append(entry.getKey()).append("==>").append(entry.getValue()).append("\n");
            }
            return Response.with(Response.Status.OK).enableGzip(true).body(builder.toString()).build();
        }
    };

    @Override
    protected boolean isWriteComplete() {
        if (response.hasRemaining() || buffer(true).hasRemaining()) {
            response.write(buffer(false));
            return false;
        }
        return true;
    }

    @Override
    public void onReceive() throws Exception {
        if (decode == null) {
            decode = new HttpDecode();
        }
        try {
            if (decode.input(buffer(true))) {
                serve(decode.request);
            }
        } catch (ResponseException e) {
            e.printStackTrace();
            response = Response.with(e.getStatus()).body(e.getMessage()).build();
        }
        sendResponse();
    }

    private void serve(Request request) throws ResponseException {
        Method method = request.getMethod();
        switch (method) {
            case GET:
                response = servlet.get(request);
                break;
            case PUT:
            case POST:
                response = servlet.post(request);
                break;
            default:
                response = Response.with(Response.Status.NOT_FOUND).contentType("text/plain").body("Not Found").build();
                break;
        }
    }

    private void sendResponse() {
        if (response != null) {
            try {
                clearBuffer();
                response.write(buffer(false));
                switchMode(false);
            } catch (ClosedChannelException e) {
                onException(e);
            }
        }
    }

    @Override
    protected void onWriteComplete() {
        safeClose(response);
        response = null;
        safeClose(decode);
        decode = null;
    }

    @Override
    protected void onClose() {
        super.onClose();
        ByteBufferUtil.recycle(buffer(true));
    }

    private static void safeClose(Object closeable) {
        try {
            if (closeable == null) {

            } else if (closeable instanceof Closeable) {
                ((Closeable) closeable).close();
            } else if (closeable instanceof Socket) {
                ((Socket) closeable).close();
            } else if (closeable instanceof ServerSocket) {
                ((ServerSocket) closeable).close();
            } else {
                throw new IllegalArgumentException("Unknown object to close");
            }
        } catch (IOException e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Could not close", e);
        }
    }

}
