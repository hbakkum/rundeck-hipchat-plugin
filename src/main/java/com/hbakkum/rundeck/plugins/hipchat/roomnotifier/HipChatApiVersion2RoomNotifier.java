package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

import com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginException;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpRequestExecutor;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;

import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.urlEncode;

/**
 * @author Hayden Bakkum
 */
public class HipChatApiVersion2RoomNotifier implements HipChatRoomNotifier {

    private static final String HIPCHAT_API_ROOM_NOTIFICATION_URL_PATH = "room/%s/notification";
    private static final String HIPCHAT_API_ROOM_NOTIFICATION_URL_QUERY = "?auth_token=%s";
    private static final String HIPCHAT_API_VERSION = "v2";

    private final HttpRequestExecutor httpRequestExecutor;

    public HipChatApiVersion2RoomNotifier(final HttpRequestExecutor httpRequestExecutor) {
        this.httpRequestExecutor = httpRequestExecutor;
    }

    @Override
    public void sendRoomNotification(
            final String baseURL,
            final String room,
            final String message,
            final String color,
            final String authToken) {

        final ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.put("message", message);
        requestBody.put("color", color);
        requestBody.put("message_format", "html");

        final String urlPath = String.format(HIPCHAT_API_ROOM_NOTIFICATION_URL_PATH, urlEncode(room));
        final String urlQueryString = String.format(HIPCHAT_API_ROOM_NOTIFICATION_URL_QUERY, urlEncode(authToken));

        final HttpResponse httpResponse = httpRequestExecutor.execute(baseURL + "/" + HIPCHAT_API_VERSION + "/" + urlPath + urlQueryString, requestBody.toString());

        if (httpResponse.getResponseCode() != HttpResponse.STATUS__NO_CONTENT) {
            throw toHipChatNotificationPluginException(httpResponse);
        }
    }

    @Override
    public String getSupportedApiVersion() {
        return HIPCHAT_API_VERSION;
    }

    private HipChatNotificationPluginException toHipChatNotificationPluginException(final HttpResponse httpResponse) {
        if (HttpResponse.CONTENT_TYPE__JSON.equals(httpResponse.getContentType())) {
            final String errorMessage = getErrorMessage(httpResponse.getResponseBody());
            if (errorMessage != null && !errorMessage.isEmpty()) {
                return new HipChatNotificationPluginException("HipChat API returned an error: ["+errorMessage+"]");
            }
        }

        return new HipChatNotificationPluginException("Unexpected response received from HipChat API: ["+httpResponse+"]");
    }

    private String getErrorMessage(final String responseBody) {
        try {
            return new ObjectMapper().readTree(responseBody).path("error").path("message").asText();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error reading HipChat API JSON response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

}
