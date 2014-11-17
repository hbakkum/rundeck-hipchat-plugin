package com.hbakkum.rundeck.plugins.hipchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

    public static URL toURL(final String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException malformedURLEx) {
            throw new HipChatNotificationPluginException("HipChat API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        }
    }

    public static HttpURLConnection openConnection(final URL requestUrl) {
        try {
            return (HttpURLConnection) requestUrl.openConnection();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error opening connection to HipChat URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    public static InputStream getResponseStream(final HttpURLConnection connection) {
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException ioEx) {
            input = connection.getErrorStream();
        }
        return input;
    }

    public static int getResponseCode(final HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Failed to obtain HTTP response from HipChat server: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    public static void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
        }
    }

    private HipChatNotificationPluginUtils() {}

}
