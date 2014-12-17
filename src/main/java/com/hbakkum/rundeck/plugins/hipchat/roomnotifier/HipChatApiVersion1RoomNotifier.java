package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

import com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginException;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpRequestExecutor;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpResponse;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Map;

import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.urlEncode;

/**
 * @author Hayden Bakkum
 */
public class HipChatApiVersion1RoomNotifier implements HipChatRoomNotifier {

    private static final String HIPCHAT_API_ROOM_NOTIFICATION_URL_PATH = "rooms/message";
    private static final String HIPCHAT_API_ROOM_NOTIFICATION_URL_QUERY = "?auth_token=%s&format=json&message_format=html&room_id=%s&from=%s&message=%s&color=%s";
    private static final String HIPCHAT_API_VERSION = "v1";

    private static final String HIPCHAT_MESSAGE_FROM_NAME = "Rundeck";

    private final HttpRequestExecutor httpRequestExecutor;

    public HipChatApiVersion1RoomNotifier(final HttpRequestExecutor httpRequestExecutor) {
        this.httpRequestExecutor = httpRequestExecutor;
    }

    @Override
    public void sendRoomNotification(
            final String baseURL,
            final String room,
            final String message,
            final String color,
            final String authToken) {

        final String urlQueryString = String.format(HIPCHAT_API_ROOM_NOTIFICATION_URL_QUERY,
                urlEncode(authToken),
                urlEncode(room),
                urlEncode(HIPCHAT_MESSAGE_FROM_NAME),
                urlEncode(message),
                urlEncode(color));

        final HipChatAPIResponse hipChatResponse = invokeHipChatAPI(baseURL, HIPCHAT_API_ROOM_NOTIFICATION_URL_PATH, urlQueryString);

        if (hipChatResponse.hasError()) {
            throw new HipChatNotificationPluginException("Error returned from HipChat API: [" + hipChatResponse.getErrorMessage() + "].");
        }

        if (!"sent".equals(hipChatResponse.status)) {
            throw new HipChatNotificationPluginException("Unknown status returned from HipChat API: [" + hipChatResponse.status + "].");
        }
    }

    @Override
    public String getSupportedApiVersion() {
        return HIPCHAT_API_VERSION;
    }

    private HipChatAPIResponse invokeHipChatAPI(final String hipchatServerBaseUrl, final String urlPath, final String urlQueryString) {
        final HttpResponse httpResponse = httpRequestExecutor.execute(hipchatServerBaseUrl + "/" + HIPCHAT_API_VERSION + "/" + urlPath + urlQueryString);

        // naively check that a HipChat API response was obtained.
        if (HttpResponse.CONTENT_TYPE__JSON.equals(httpResponse.getContentType())) {
            return toHipChatResponse(httpResponse.getResponseBody());
        } else {
            throw new HipChatNotificationPluginException("Request did not reach HipChat API. Response code was [" + httpResponse.getResponseCode() + "]. Are your proxy settings correct?");
        }
    }

    private HipChatAPIResponse toHipChatResponse(final String responseBody) {
        try {
            return new ObjectMapper().readValue(responseBody, HipChatAPIResponse.class);
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error reading HipChat API JSON response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HipChatAPIResponse {

        @JsonProperty
        private String status;

        @JsonProperty
        private Map<String, Object> error;

        private boolean hasError() {
            return error != null;
        }

        private String getErrorMessage() {
            return (String) error.get("message");
        }

    }

}
