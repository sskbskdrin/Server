package cn.ssbskdrin.server.files.http;

/**
 * Created by keayuan on 2021/4/12.
 *
 * @author keayuan
 */
class HttpDispatch {
    static HttpDecode get(Request request) {
        return new HttpDecode();
        //        sendResponse.send(Response.newFixedLengthResponse("this is test get"));
    }
}
