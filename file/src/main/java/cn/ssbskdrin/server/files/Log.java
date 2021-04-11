package cn.ssbskdrin.server.files;

/**
 * Created by sskbskdrin on 2021/4/11.
 *
 * @author sskbskdrin
 */
interface Log {

    default void logP(Object... obj) {
        System.out.print(obj[0]);
    }

    default void log(Object... obj) {
        System.out.println(obj[0]);
    }
}
