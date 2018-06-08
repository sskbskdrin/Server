package cn.sskbskdrin.server.ftp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Created by ex-keayuan001 on 2018/6/8.
 *
 * @author ex-keayuan001
 */
public class FtpServerTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void start() {
        FtpServer.getInstance().start(2121);
    }

    @Test
    public void stop() {
        Pattern pattern = Pattern.compile(REGEX);
        MatchResult result = pattern.matcher("drw-r--r-- s ftp ftp 751 2018-05-21 10:47 proguard-rules.pro")
            .toMatchResult();
        String typeStr = result.group(1);
        String hardLinkCount = result.group(15);
        String usr = result.group(16);
        String grp = result.group(17);
        String filesize = result.group(18);
        String datestr = result.group(19) + " " + result.group(20);
        String name = result.group(21);
        String endtoken = result.group(22);
        for (int i = 0; i < result.groupCount(); i++) {
            System.out.println(result.group(i));
        }
    }

    private static final String REGEX = "([bcdelfmpSs-])" + "(((r|-)(w|-)([xsStTL-]))((r|-)(w|-)([xsStTL-]))((r|-)" +
        "(w|-)([xsStTL-])))\\+?\\s*" + "(\\d+)\\s+"                                  // link count
        + "(?:(\\S+(?:\\s\\S+)*?)\\s+)?"                // owner name (optional spaces)
        + "(?:(\\S+(?:\\s\\S+)*)\\s+)?"                 // group name (optional spaces)
        + "(\\d+(?:,\\s*\\d+)?)\\s+"                    // size or n,m
        /*
         * numeric or standard format date:
         *   yyyy-mm-dd (expecting hh:mm to follow)
         *   MMMM [d]d
         *   [d]d MMM
         */ + "((?:\\d+[-/]\\d+[-/]\\d+)|(?:[a-zA-Z]{3}\\s+\\d{1,2})|(?:\\d{1,2}\\s+[a-zA-Z]{3}))\\s+"
        /*
           year (for non-recent standard format) - yyyy
           or time (for numeric or recent standard format) [h]h:mm
        */ + "(\\d+(?::\\d+)?)\\s+"

        + "(\\S*)(\\s*.*)"; // the rest
}