package cn.sskbskdrin.server.rstp;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;

/**
 * @author sskbskdrin
 * @date 2019/April/24
 */
class RTSPHandler {

    static HttpResponse handler(HttpRequest request) {
        DefaultHttpResponse response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        switch (request.method().name().toUpperCase()) {
            case "OPTIONS":
                options(request, response);
                break;
            case "DESCRIBE":
                break;
            case "SETUP":
                break;
            case "PLAY":
                break;
            case "PAUSE":
                break;
            case "TEARDOWN":
                break;
            case "GET_PARAMETER":
                break;
            case "SET_PARAMETER":
                break;
            case "REDIRECT":
                break;
            case "ANNOUNCE":
                break;
            case "RECORD":
                break;
        }
        return response;
    }

    private static void options(HttpRequest request, HttpResponse response) {
        HttpHeaders headers = response.headers();
        headers.add(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
        headers.add(RtspHeaderNames.PUBLIC, "DESCRIBE,SETUP,PLAY,PAUSE,TEARDOWN,GET_PARAMETER,SET_PARAMETER,REDIRECT,"
            + "ANNOUNCE");
    }

    private static void describe(HttpRequest request, HttpResponse response) {

    }

    private static void setup(HttpRequest request, HttpResponse response) {
    }


}
