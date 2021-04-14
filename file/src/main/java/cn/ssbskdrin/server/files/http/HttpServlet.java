package cn.ssbskdrin.server.files.http;

/**
 * Created by keayuan on 2021/4/14.
 *
 * @author keayuan
 */
interface HttpServlet {
    Response get(Request request) throws ResponseException;

    Response post(Request request) throws ResponseException;
}
