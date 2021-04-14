package cn.ssbskdrin.server.files;

import cn.ssbskdrin.server.files.core.NioServer;
import cn.ssbskdrin.server.files.http.HttpHandler;

/**
 * Created by keayuan on 2021/4/9.
 *
 * @author keayuan
 */
public class Main {
    public static void main(String[] args) {
        NioServer.bind(8080).channelContextProvider(HttpHandler::new).build().sync();
    }
}
