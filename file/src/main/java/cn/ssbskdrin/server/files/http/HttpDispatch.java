package cn.ssbskdrin.server.files.http;

/**
 * Created by keayuan on 2021/4/12.
 *
 * @author keayuan
 */
class HttpDispatch {
    static HttpDecoder get(Request request) {
        return new HttpDecoder();
        //        sendResponse.send(Response.newFixedLengthResponse("this is test get"));
    }
}
