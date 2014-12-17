package com.hbakkum.rundeck.plugins.hipchat.http;

import com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginException;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;
import us.monoid.web.TextResource;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.getResponseCode;
import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.isNotEmpty;
import static us.monoid.web.Resty.content;

/**
 * @author Hayden Bakkum
 */
public class RestyHttpRequestExecutor implements HttpRequestExecutor {

    private String proxyHost;
    private int proxyPort = -1;

    @Override
    public void setProxy(final String proxyHost, final int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    @Override
    public HttpResponse execute(final String url) {
        HttpURLConnection httpConnection = null;
        try {
            final Resty resty = new Resty();
            if (isProxySet()) {
                resty.setProxy(proxyHost, proxyPort);
            }
            final TextResource textResource = resty.text(url);
            httpConnection = textResource.http();

            return toHttpResponse(textResource, httpConnection);

        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error opening connection to HipChat URL: [" + ioEx.getMessage() + "].", ioEx);

        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    @Override
    public HttpResponse execute(final String url, final String jsonRequestBody) {
        HttpURLConnection httpConnection = null;
        try {
            final Resty resty = new Resty();
            if (isProxySet()) {
                resty.setProxy(proxyHost, proxyPort);
            }
            final TextResource textResource = resty.text(url, content(new JSONObject(jsonRequestBody)));
            httpConnection = textResource.http();

            return toHttpResponse(textResource, httpConnection);

        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error opening connection to HipChat URL: [" + ioEx.getMessage() + "].", ioEx);

        } catch (JSONException jsonEx) {
            throw new HipChatNotificationPluginException("Malformed JSON request body: [" + jsonEx.getMessage() + "].", jsonEx);

        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    private boolean isProxySet() {
        return isNotEmpty(proxyHost) && proxyPort > -1;
    }

    private HttpResponse toHttpResponse(final TextResource textResource, final HttpURLConnection httpConnection) {
        final int responseCode = getResponseCode(httpConnection);
        final String contentType = httpConnection.getHeaderField("content-type");
        final String responseBody = textResource.toString();

        return new HttpResponse(responseCode, contentType, responseBody);
    }

}
