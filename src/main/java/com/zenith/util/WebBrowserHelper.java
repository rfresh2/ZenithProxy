package com.zenith.util;

import com.zenith.Shared;

import java.io.IOException;

// https://www.geekyhacker.com/open-a-url-in-the-default-browser-in-java/
public class WebBrowserHelper {

    public static void openBrowser(String url) {
        try {
            if (isMacOperatingSystem()) {
                openUrlInDefaultMacOsBrowser(url);
            } else if (isWindowsOperatingSystem()) {
                openUrlInDefaultWindowsBrowser(url);
            }
            // not even gonna try to support linux lol
        } catch (Exception e) {
            Shared.DEFAULT_LOG.debug("Failed to open url in browser", e);
        }
    }

    private static boolean isMacOperatingSystem() {
        return getOperatingSystemName().toLowerCase().startsWith("mac");
    }

    private static boolean isWindowsOperatingSystem() {
        return getOperatingSystemName().toLowerCase().startsWith("windows");
    }

    private static String getOperatingSystemName() {
        return System.getProperty("os.name");
    }

    private static void openUrlInDefaultMacOsBrowser(String url) throws IOException {
        Runtime.getRuntime().exec(new String[]{"open", url});
    }

    private static void openUrlInDefaultWindowsBrowser(String url) throws IOException {
        Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
    }
}
