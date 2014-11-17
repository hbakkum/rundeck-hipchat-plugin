package com.hbakkum.rundeck.plugins.hipchat.http;

/**
 * @author Hayden Bakkum
 */
public interface HttpRequestExecutor {

    HttpResponse execute(final String url);

    HttpResponse execute(final String url, final String jsonRequestBody);

}
