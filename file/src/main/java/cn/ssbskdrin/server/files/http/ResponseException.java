package cn.ssbskdrin.server.files.http;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
public class ResponseException extends Exception {
    private final Response.Status status;

    public ResponseException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public ResponseException(Response.Status status, String message, Exception e) {
        super(message, e);
        this.status = status;
    }

    public Response.Status getStatus() {
        return this.status;
    }
}
