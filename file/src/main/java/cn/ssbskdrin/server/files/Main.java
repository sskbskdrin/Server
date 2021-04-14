package cn.ssbskdrin.server.files;

import cn.ssbskdrin.server.files.core.NioServer;
import cn.ssbskdrin.server.files.http.KMP;
import cn.sskbskdrin.log.L;

/**
 * Created by keayuan on 2021/4/9.
 *
 * @author keayuan
 */
public class Main {
    public static void main(String[] args) {
        NioServer.main(args);
        String content = "aaaafaaaafaaaafaafaafaaaa";
        String mod = "aaaafaafaa";
        KMP kmp = new KMP(mod.getBytes());
        for (byte b : content.getBytes()) {
            int pos = kmp.input(b);
            if (pos >= 0) {
                L.d("pos=" + pos);
            }
        }
        int pos = -mod.length();
        while ((pos = content.indexOf(mod, pos + mod.length())) >= 0) {
            System.out.println(pos);
        }
    }
}
