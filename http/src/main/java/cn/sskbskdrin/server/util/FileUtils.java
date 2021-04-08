package cn.sskbskdrin.server.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by keayuan on 2021/4/8.
 *
 * @author keayuan
 */
public final class FileUtils {
    private FileUtils() {}

    public static class Properties {
        private String name;
        private long time;
        private boolean isFile;
        private long size;

        Properties(File file) {
            if (file == null) return;
            name = file.getName();
            time = file.lastModified();
            isFile = file.isFile();
            size = isFile ? file.length() : 0;
        }

        public String getName() {
            return name;
        }

        public long getTime() {
            return time;
        }

        public boolean isFile() {
            return isFile;
        }

        public String getExt() {
            return FileUtils.getExt(name == null ? "" : name);
        }

        public long getSize() {
            return size;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("name", name);
            object.put("time", time);
            object.put("isFile", isFile);
            object.put("ext", getExt());
            object.put("size", getSize());
            return object;
        }
    }

    public static Properties getProperties(File file) {
        if (file == null) {
            return null;
        }
        return new Properties(file);
    }

    public static String getExt(String file) {
        int in = file.lastIndexOf('.');
        if (in > 0) return file.substring(in + 1);
        else return null;
    }

    public static String getExt(File file) {
        if (file.isDirectory()) return "";
        return getExt(file.getName());
    }
}
