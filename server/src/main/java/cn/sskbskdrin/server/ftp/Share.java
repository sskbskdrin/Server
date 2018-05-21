package cn.sskbskdrin.server.ftp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 所有线程共享的变量
 */
public class Share {

    /*根目录的路径*/
    public static String rootDir = System.getProperty("user.dir");

    /*允许登录的用户*/
    public static Map<String, String> users = new HashMap<>();

    /*已经登录的用户*/
    public static HashSet<String> loginedUser = new HashSet<>();

    /*拥有权限的用户*/
    public static HashSet<String> adminUsers = new HashSet<>();

    //初始化根目录，权限用户，能够登录的用户信息
    static {
        users.put("kay", "123456");
        adminUsers.add("kay");
    }
}
