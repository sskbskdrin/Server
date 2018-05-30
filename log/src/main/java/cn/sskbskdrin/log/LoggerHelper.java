package cn.sskbskdrin.log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

class LoggerHelper implements LogHelper {

    /**
     * It is used for json pretty print
     */
    private static final int JSON_INDENT = 2;

    /**
     * Provides one-time used tag for the log message
     */
    private String localTag = "";

    private final Set<Printer> logPrinters = new HashSet<>();

    @Override
    public LogHelper tag(String tag) {
        localTag = tag;
        return this;
    }

    public String json(String json) {
        if (json == null || json.length() == 0) {
            json = "";
        }
        try {
            json = json.trim();
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject.toString(JSON_INDENT);
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                return jsonArray.toString(JSON_INDENT);
            }
            return json;
        } catch (JSONException e) {
        }
        return json;
    }

    public String xml(String xml) {
        if (xml == null || xml.length() == 0) {
            xml = "";
        }
        try {
            xml = xml.trim();
            if (xml.startsWith("<")) {
                System.out.println("xml len=" + xml.length());
                Source xmlInput = new StreamSource(new StringReader(xml));
                StreamResult xmlOutput = new StreamResult(new StringWriter());
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(xmlInput, xmlOutput);
                xml = xmlOutput.getWriter().toString().replaceFirst(">", ">\n");
            }
        } catch (TransformerException e) {
        }
        return xml;
    }

    @Override
    public synchronized void log(int priority, String tag, String message, Throwable throwable) {
        if (throwable != null) {
            if (message == null) {
                message = "";
            }
            message += "\n" + getStackTraceString(throwable);
        } else {
            message = json(message);
            message = xml(message);
        }
        if (message == null || message.length() == 0) {
            message = "Empty/NULL log message";
        }
        if (useGlobalTag()) {
            if (tag != null && tag.length() > 0) {
                message = tag + ": " + message;
            }
            tag = localTag;
        }
        for (Printer printer : logPrinters) {
            if (printer.isLoggable(priority, tag)) {
                printer.log(priority, tag, message);
            }
        }
    }

    @Override
    public void clearAdapters() {
        logPrinters.clear();
    }

    @Override
    public boolean useGlobalTag() {
        return localTag != null && localTag.length() > 0;
    }

    @Override
    public void addAdapter(Printer adapter) {
        if (!logPrinters.contains(adapter)) {
            logPrinters.add(adapter);
        }
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

}
