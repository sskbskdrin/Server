package cn.ssbskdrin.server.files.http;

/**
 * Created by keayuan on 2021/4/12.
 *
 * @author keayuan
 */
class HttpDispatch {
    static HttpServlet get(Request request) {
        return new HttpServlet();
        //        sendResponse.send(Response.newFixedLengthResponse("this is test get"));
    }
}
