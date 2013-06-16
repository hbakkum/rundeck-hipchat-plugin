/*
 * Copyright 2013 Hayden Bakkum
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.rundeck.plugin.hipchat;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Plugin(service= "Notification", name="HipChatNotification")
@PluginDescription(title="HipChat")
public class HipChatNotificationPlugin implements NotificationPlugin {

    private static final String HIPCHAT_API_BASE = "https://api.hipchat.com/v1/";
    private static final String HIPCHAT_API_MESSAGE_ROOM_METHOD = "rooms/message";
    private static final String HIPCHAT_API_MESSAGE_ROOM_QUERY = "?auth_token=%s&format=json&message_format=html&room_id=%s&from=%s&message=%s&color=%s";

    private static final String HIPCHAT_MESSAGE_COLOR_GREEN = "green";
    private static final String HIPCHAT_MESSAGE_COLOR_YELLOW = "yellow";
    private static final String HIPCHAT_MESSAGE_COLOR_RED = "red";

    private static final String HIPCHAT_MESSAGE_FROM_NAME = "Rundeck";
    private static final String HIPCHAT_MESSAGE_TEMPLATE = "hipchat-message.ftl";

    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_SUCCESS = "success";
    private static final String TRIGGER_FAILURE = "failure";

    private static final Configuration FREEMARKER_CFG = new Configuration();
    static {
        FREEMARKER_CFG.setClassForTemplateLoading(HipChatNotificationPlugin.class, "/templates");
    }

    private static final Map<String, HipChatNotificationData> TRIGGER_NOTIFICATION_DATA = new HashMap<String, HipChatNotificationData>();
    static {
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_START, new HipChatNotificationData(HIPCHAT_MESSAGE_TEMPLATE, HIPCHAT_MESSAGE_COLOR_YELLOW));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_SUCCESS, new HipChatNotificationData(HIPCHAT_MESSAGE_TEMPLATE, HIPCHAT_MESSAGE_COLOR_GREEN));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_FAILURE, new HipChatNotificationData(HIPCHAT_MESSAGE_TEMPLATE, HIPCHAT_MESSAGE_COLOR_RED));
    }

    @PluginProperty(
            title = "Room",
            description = "HipChat room to send notification message to.",
            required = true)
    private String room;

    @PluginProperty(
            title = "API Auth Token",
            description = "HipChat API authentication token. Notification level token will do.",
            required = true)
    private String apiAuthToken;

    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {
        if (!TRIGGER_NOTIFICATION_DATA.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        String hipChatMessageRoomQuery = createHipChatAPIMessageRoomQuery(trigger, executionData, config);

        HipChatAPIResponse hipChatResponse = sendHipChatAPIQuery(hipChatMessageRoomQuery);
        if (hipChatResponse.error != null) {
            throw new HipChatNotificationPluginException("Error returned from HipChat API: [" + hipChatResponse.error.get("message") + "].");
        }

        if ("sent".equals(hipChatResponse.status)) {
            return true;
        } else {
            throw new HipChatNotificationPluginException("Unknown status returned from HipChat API: [" + hipChatResponse.status + "].");
        }
    }

    private String createHipChatAPIMessageRoomQuery(String trigger, Map executionData, Map config) {
        String color = TRIGGER_NOTIFICATION_DATA.get(trigger).color;
        String message = generateMessage(trigger, executionData, config);
        String query = String.format(HIPCHAT_API_MESSAGE_ROOM_QUERY,
                urlEncode(apiAuthToken),
                urlEncode(room),
                urlEncode(HIPCHAT_MESSAGE_FROM_NAME),
                urlEncode(message),
                urlEncode(color));

        return HIPCHAT_API_MESSAGE_ROOM_METHOD + query;
    }

    private String generateMessage(String trigger, Map executionData, Map config) {
        String templateName = TRIGGER_NOTIFICATION_DATA.get(trigger).template;

        Map<String, Object> model = new HashMap();
        model.put("trigger", trigger);
        model.put("executionData", executionData);
        model.put("config", config);

        StringWriter sw = new StringWriter();
        try {
            Template template = FREEMARKER_CFG.getTemplate(templateName);
            template.process(model,sw);
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error loading HipChat notification message template: [" + ioEx.getMessage() + "].", ioEx);
        } catch (TemplateException templateEx) {
            throw new HipChatNotificationPluginException("Error merging HipChat notification message template: [" + templateEx.getMessage() + "].", templateEx);
        }

        return sw.toString();
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new HipChatNotificationPluginException("URL encoding error: [" + unsupportedEncodingException.getMessage() + "].", unsupportedEncodingException);
        }
    }

    private HipChatAPIResponse sendHipChatAPIQuery(String query) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            URL requestUrl = new URL(HIPCHAT_API_BASE + query);
            connection = (HttpURLConnection) requestUrl.openConnection();
            input = getResponseStream(connection);

            return new ObjectMapper().readValue(input, HipChatAPIResponse.class);

        } catch (MalformedURLException malformedURLEx) {
            throw new HipChatNotificationPluginException("HipChat API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("IO error occurred while communicating with HipChat API: [" + ioEx.getMessage() + "].", ioEx);
        } finally {
            closeAndDisconnectQuietly(input, connection);
        }
    }

    private InputStream getResponseStream(HttpURLConnection connection) {
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException ioEx) {
            input = connection.getErrorStream();
        }

        return input;
    }

    private void closeAndDisconnectQuietly(InputStream input, HttpURLConnection connection) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
        }

        if (connection != null) {
            connection.disconnect();
        }
    }

    private static class HipChatNotificationData {
        private String template;
        private String color;
        public HipChatNotificationData(String template, String color) {
            this.template = template;
            this.color = color;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HipChatAPIResponse {
        @JsonProperty private String status;
        @JsonProperty private Map<String, Object> error;
    }

}
