package cn.sskbskdrin.server.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Created by ex-keayuan001 on 2018/1/23.
 *
 * @author ex-keayuan001
 */
public class NettyClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8888"));

    public static void main(String[] args) {
        boolean flag = true;
        if (flag) {
            try {
                long start = System.currentTimeMillis();
                doGet();
                System.out.println("time=" + (System.currentTimeMillis() - start));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }
            //            test();
            //            testAppProcess();
            return;
        }
        ChannelThread thread = new ChannelThread(HOST, PORT, null);
        thread.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Channel channel = thread.channel;

        byte[] buff = new byte[1024];
        while (true) {
            try {
                int count = System.in.read(buff);
                if (count > 0) {
                    String content = new String(buff, 0, count - 1);
                    if (content.equals("exit")) {
                        break;
                    }
                    Body body = new Body();
                    body.head = 0xCCBB;
                    body.flag = 0x3300;
                    StringBuilder builder = new StringBuilder();
                    builder.append(content);
                    body.data = builder.toString().getBytes();
                    body.crc = 0x30;
                    if (channel.isActive()) {
                        channel.writeAndFlush(body);
                    } else {
                        channel.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        channel.close();
    }

    private static void test() {
        //        try {
        //            Runtime.getRuntime().exec("adb shell export CLASSPATH=/data/app/cn.sskbskdrin.floatview-1/base
        // .apk\n" +
        //                    "exec app_process32 /system/bin cn.sskbskdrin.floatview.Main");
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
        try {
            Process process = Runtime.getRuntime().exec("adb shell");  //adb shell

            final StringBuilder cmd = new StringBuilder("export CLASSPATH=/data/app/cn.sskbskdrin.floatview-1/base" +
                ".apk \n");
            cmd.append("exec app_process32 /system/bin cn.sskbskdrin.floatview.Main \" 12345\" \n");
            //            cmd.append("exit\n");

            final BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
            //这里一定要注意错误流的读取，不然很容易阻塞，得不到你想要的结果，
            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            new Thread(new Runnable() {
                String line;

                public void run() {
                    System.out.println("listener started");
                    try {
                        while ((line = inputStream.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            new Thread(new Runnable() {
                final BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cmd
                    .toString().getBytes())));

                public void run() {
                    System.out.println("writer started");
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            outputStream.write(line + "\r\n");
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            int i = process.waitFor();
            System.out.println("i=" + i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int testAppProcess() {
        int status = 0;
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("adb shell");// 切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(" export CLASSPATH=/data/app/cn.sskbskdrin.server-1/base.apk \n");
            os.writeBytes(" exec app_process32 /system/bin cn.sskbskdrin.server.Main \n");
            os.writeBytes(" exit \n");
            os.flush();
            // waitFor返回的退出值的过程。按照惯例，0表示正常终止。waitFor会一直等待
            status = process.waitFor();// 什么意思呢？具体看http://my.oschina.net/sub/blog/134436
        } catch (Exception e) {
            status = -2;
            return status;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
            }
        }
        return status;
    }

    public static void connect(String host, int port, ChannelCallback callback) {
        new ChannelThread(host, port, callback).start();
    }

    private static class ChannelThread extends Thread {

        private Channel channel;
        private ChannelCallback callback;
        private String host;
        private int port;

        ChannelThread(String host, int port, ChannelCallback callback) {
            this.host = host;
            this.port = port;
            this.callback = callback;
        }

        @Override
        public void run() {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(new
                    BodyChannelInitializer());

                ChannelFuture future = b.connect(host, port).sync();
                channel = future.channel();
                if (callback != null) {
                    callback.onConnect(channel);
                }
                channel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (callback != null) {
                    callback.onDisconnect(channel);
                }
                group.shutdownGracefully();
            }
        }
    }

    public interface ChannelCallback {
        void onConnect(Channel channel);

        void onDisconnect(Channel channel);
    }

    /**
     * Get Request
     *
     * @return
     * @throws Exception
     */
    public static String doGet() throws Exception {
        URL localURL = new URL("http://localhost:8080/");
        URLConnection connection = localURL.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

        httpURLConnection.setRequestProperty("Accept-Charset", "utf-8");
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer resultBuffer = new StringBuffer();
        String tempLine = null;

        if (httpURLConnection.getResponseCode() >= 300) {
            throw new Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
        }
        byte[] bytes;
        try {
            inputStream = httpURLConnection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bytes = new byte[1024 * 1024];
            inputStream.read(bytes);
            //            reader = new BufferedReader(inputStreamReader);
            //
            //            out.write(str.getBytes());
            //            byte[] buff = new byte[1024];
            //            int count = input.read(buff);
            //
            //            while ((tempLine = reader.readLine()) != null) {
            //                resultBuffer.append(tempLine);
            //            }

        } finally {

            if (reader != null) {
                reader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }

        }

        return new String(bytes);
    }

}
