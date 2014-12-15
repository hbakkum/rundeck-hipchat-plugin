package com.hbakkum.rundeck.plugins.hipchat.http;

/**
 * @author Hayden Bakkum
 */
public interface HttpRequestExecutor {

    void setProxyHost(final String proxyHost);

    void setProxyPort(final int proxyPort);

    HttpResponse execute(final String url);

    HttpResponse execute(final String url, final String jsonRequestBody);

}
