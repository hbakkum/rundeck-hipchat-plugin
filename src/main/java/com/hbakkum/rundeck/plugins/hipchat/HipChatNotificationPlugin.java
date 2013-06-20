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

package com.hbakkum.rundeck.plugins.hipchat;

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

import java.io.*;
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

        String color = TRIGGER_NOTIFICATION_DATA.get(trigger).color;
        String message = generateMessage(trigger, executionData, config);
        String params = String.format(HIPCHAT_API_MESSAGE_ROOM_QUERY,
                urlEncode(apiAuthToken),
                urlEncode(room),
                urlEncode(HIPCHAT_MESSAGE_FROM_NAME),
                urlEncode(message),
                urlEncode(color));

        HipChatAPIResponse hipChatResponse = invokeHipChatAPIMethod(HIPCHAT_API_MESSAGE_ROOM_METHOD, params);

        if (hipChatResponse.hasError()) {
            throw new HipChatNotificationPluginException("Error returned from HipChat API: [" + hipChatResponse.getErrorMessage() + "].");
        }

        if ("sent".equals(hipChatResponse.status)) {
            return true;
        } else {
            throw new HipChatNotificationPluginException("Unknown status returned from HipChat API: [" + hipChatResponse.status + "].");
        }
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

    private HipChatAPIResponse invokeHipChatAPIMethod(String method, String params) {
        URL requestUrl = toURL(HIPCHAT_API_BASE + method + params);

        HttpURLConnection connection = null;
        InputStream responseStream = null;
        try {
            connection = openConnection(requestUrl);
            responseStream = getResponseStream(connection);
            int responseCode = getResponseCode(connection);

            // naively check that a HipChat API response was obtained.
            if ("application/json".equals(connection.getHeaderField("content-type"))) {
                return toHipChatResponse(responseStream);
            } else {
                throw new HipChatNotificationPluginException("Request did not reach HipChat API. Response code was [" + responseCode + "]. Are your proxy settings correct?");
            }

        } finally {
            closeQuietly(responseStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException malformedURLEx) {
            throw new HipChatNotificationPluginException("HipChat API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        }
    }

    private HttpURLConnection openConnection(URL requestUrl) {
        try {
            return (HttpURLConnection) requestUrl.openConnection();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error opening connection to HipChat URL: [" + ioEx.getMessage() + "].", ioEx);
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

    private int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Failed to obtain HTTP response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private HipChatAPIResponse toHipChatResponse(InputStream responseStream) {
        try {
            return new ObjectMapper().readValue(responseStream, HipChatAPIResponse.class);
        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error reading HipChat API JSON response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
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

        private boolean hasError() {
            return error != null;
        }

        private String getErrorMessage() {
            return (String) error.get("message");
        }
    }

}
