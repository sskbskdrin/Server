package cn.sskbskdrin.server.servlet.file;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import cn.sskbskdrin.log.L;
import cn.sskbskdrin.server.annotation.API;
import cn.sskbskdrin.server.http.HandlerServlet;
import cn.sskbskdrin.server.util.ResponseFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.stream.ChunkedFile;

/**
 * Created by keayuan on 2020/9/8.
 *
 * @author keayuan
 */
@API("/files")
public class Files implements HandlerServlet {
    private static final String TAG = "Files";
    private static final String ROOT_PATH = "/sdcard/";
    public static String indexHtml;

    @Override
    public HttpResponse get(ChannelHandlerContext ctx, HttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String path = decoder.path();
        if ("/files".equalsIgnoreCase(path)) { // 加载页面
            return ResponseFactory.successHtml(indexHtml);
        } else if (path.startsWith("/files/api")) {
            path = (ROOT_PATH + path.substring(10)).replaceAll("//", "/");
        }
        File file = new File(path);

        if (!file.exists()) {
            return ResponseFactory.code("404", "文件不存在");
        }
        if (file.isDirectory()) { // 文件列表
            JSONObject ret = new JSONObject();
            JSONArray array = new JSONArray();
            try {
                for (File listFile : file.listFiles()) {
                    JSONObject object = new JSONObject();
                    object.put("name", listFile.getName());
                    object.put("time", listFile.lastModified());
                    object.put("isFile", listFile.isFile());
                    object.put("ext", getExt(listFile));
                    object.put("size", listFile.isFile() ? listFile.length() : 0);
                    object.put("name", listFile.getName());
                    array.put(object);
                }
                ret.put("code", 200);
                ret.put("msg", "success");
                ret.put("data", array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return ResponseFactory.successJson(ret);
        }

        // 下载文件
        transferFile(file, ctx, request);
        return null;
    }

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(true);

    @Override
    public HttpResponse post(ChannelHandlerContext ctx, HttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String path = decoder.rawPath();
        if (path.startsWith("/files/api")) {
            path = (ROOT_PATH + path.substring(10)).replaceAll("//", "/");
        }
        try {
            HttpPostMultipartRequestDecoder httpDecoder = new HttpPostMultipartRequestDecoder(factory, request);
            httpDecoder.setDiscardThreshold(0);
            if (request instanceof LastHttpContent) {
                writeChunk(httpDecoder, path);
                httpDecoder.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseFactory.code("-1", e.getLocalizedMessage());
        }
        return ResponseFactory.code("200", "success");
    }

    private void writeChunk(HttpPostMultipartRequestDecoder httpDecoder, String path) throws IOException {
        while (httpDecoder.hasNext()) {
            InterfaceHttpData data = httpDecoder.next();
            Log.d(TAG, "writeChunk: ");
            if (data != null && InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType())) {
                final FileUpload fileUpload = (FileUpload) data;
                final File file = new File(path + "/" + fileUpload.getFilename());
                if (file.exists()) file.delete();
                file.createNewFile();
                FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel();
                FileChannel outputChannel = new FileOutputStream(file).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                outputChannel.close();
                inputChannel.close();
            }
        }
    }

    /**
     * 传输文件
     *
     * @param file
     * @param ctx
     */
    private void transferFile(File file, ChannelHandlerContext ctx, HttpRequest request) {
        try {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpHeaders heads = response.headers();
            heads.set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            heads.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream; charset=UTF-8");
            if (HttpUtil.isKeepAlive(request)) {
                heads.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            //            heads.add(HttpHeaderNames.CONTENT_DISPOSITION,
            //                "attachment; filename=\"" + URLEncoder.encode(file.getName(), "UTF-8") + "\";");
            ctx.write(response);

            ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(file, 1024 * 10), ctx.newProgressivePromise());
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    L.i(TAG, "file {} transfer complete.", file.getName());
                }

                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    if (total < 0) {
                        L.w(TAG, "file {} transfer progress: {}", file.getName(), progress);
                    } else {
                        L.d(TAG, "file {} transfer progress: {}/{}", file.getName(), progress, total);
                    }
                }
            });
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getExt(File file) {
        if (file.isDirectory()) return "";
        String name = file.getName();
        int in = name.lastIndexOf('.');
        if (in > 0) return name.substring(in + 1);
        else return null;
    }

}
