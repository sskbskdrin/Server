package cn.ssbskdrin.server.files.http;

import java.nio.ByteBuffer;

/**
 * Created by keayuan on 2021/4/12.
 *
 * @author keayuan
 */
class HttpDispatch {
    static void get(Request request, SendResponse sendResponse) {
        sendResponse.send(Response.newFixedLengthResponse("this is test get"));
    }

    static void post(Request request, SendResponse sendResponse, ByteBuffer buffer) {
        Response rsp = Response.newFixedLengthResponse("this is test post");
        rsp.setKeepAlive(true);
        rsp.closeConnection(false);
        sendResponse.send(rsp);
    }

}
