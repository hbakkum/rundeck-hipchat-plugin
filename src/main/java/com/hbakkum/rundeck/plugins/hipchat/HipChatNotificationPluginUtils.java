package com.hbakkum.rundeck.plugins.hipchat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

/**
 * @author Hayden Bakkum
 */
public final class HipChatNotificationPluginUtils {

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new HipChatNotificationPluginException("URL encoding error: [" + unsupportedEncodingException.getMessage() + "].", unsupportedEncodingException);
        }
    }

    public static int getResponseCode(final HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Failed to obtain HTTP response from HipChat server: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    public static boolean isNotEmpty(final String value) {
        return value != null && !"".equals(value);
    }

    private HipChatNotificationPluginUtils() {}

}
