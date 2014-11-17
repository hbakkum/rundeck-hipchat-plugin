package com.hbakkum.rundeck.plugins.hipchat.http;

/**
 * @author Hayden Bakkum
 */
public class HttpResponse {

    public static final int STATUS__NO_CONTENT = 204;

    public static final String CONTENT_TYPE__JSON = "application/json";

    private final int responseCode;

    private final String contentType;

    private final String responseBody;

    public HttpResponse(final int responseCode, final String contentType, final String responseBody) {
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.responseBody = responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getContentType() {
        return contentType;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return
                "HttpResponse ["+
                        "responseCode = "+responseCode+","+
                        "contentType = "+contentType+","+
                        "responseBody = "+responseBody+
                "]";
    }

}
