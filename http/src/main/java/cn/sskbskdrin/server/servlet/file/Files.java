package cn.sskbskdrin.server.servlet.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import cn.sskbskdrin.server.annotation.API;
import cn.sskbskdrin.server.http.HandlerServlet;
import cn.sskbskdrin.server.util.Html;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

/**
 * Created by keayuan on 2020/9/8.
 *
 * @author keayuan
 */
@API("/files")
public class Files implements HandlerServlet {

    @Override
    public HttpResponse get(HttpRequest request) {
        String path = request.uri().replaceFirst("/files[/]*", "");
        File file = new File("/sdcard/" + path);

        String html = new Html().header(header -> {
            header.meta("charset=\"utf-8\"");
            header.title("HTTP");
        }).body(body -> {
            body.form(form -> {
                form.input("file", "file");
                form.input("submit", "send");
            }, "/files/" + path, "multipart/form-data", "POST");
            body.br();

            if (path.length() == 0) {
                body.text("/");
            } else {
                String[] names = path.split("/");
                StringBuilder builder = new StringBuilder("/files/");
                for (String s : names) {
                    if (s.length() == 0) continue;
                    builder.append(s);
                    body.text("/");
                    body.a(s, builder.toString(), "_self");
                    builder.append('/');
                }
            }
            body.br();
            body.br();
            String parent = file.getParent().replaceFirst("/sdcard[/]*", "");
            body.a("..", "/files/" + parent, "_self");
            body.br();
            if (file.isDirectory()) {
                for (File listFile : file.listFiles()) {
                    String name = listFile.getName();
                    body.a(name, "/files/" + (path.length() == 0 ? "" : path + "/") + name, "_self");
                    body.br();
                }
            }
        }).toString();

        ByteBuf content = Unpooled.wrappedBuffer(html.getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        heads.add(HttpHeaderNames.CONTENT_TYPE, TEXT_HTML + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }

    @Override
    public HttpResponse post(HttpRequest request) {
        HttpDataFactory factory = new DefaultHttpDataFactory(true);
        HttpPostRequestDecoder httpDecoder = new HttpPostRequestDecoder(factory, request);
        httpDecoder.setDiscardThreshold(0);
        try {
            final HttpContent chunk = (HttpContent) request;
            httpDecoder.offer(chunk);
            String path = request.uri().replaceFirst("/files[/]*", "");
            if (chunk instanceof LastHttpContent) {
                writeChunk(httpDecoder, path);
                httpDecoder.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuf content = Unpooled.wrappedBuffer("OK".getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        HttpHeaders heads = response.headers();
        heads.add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        heads.add(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN + "; charset=UTF-8");
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }

    private void writeChunk(HttpPostRequestDecoder httpDecoder, String path) throws IOException {
        while (httpDecoder.hasNext()) {
            InterfaceHttpData data = httpDecoder.next();
            if (data != null && InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType())) {
                final FileUpload fileUpload = (FileUpload) data;
                final File file = new File("/sdcard/" + path + "/" + fileUpload.getFilename());
                if (file.exists()) file.delete();
                file.createNewFile();
                FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel();
                FileChannel outputChannel = new FileOutputStream(file).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                outputChannel.close();
                //                outputChannel.write(fileUpload.getByteBuf().nioBuffer());
                //                outputChannel.close();
            }
        }
    }

}
