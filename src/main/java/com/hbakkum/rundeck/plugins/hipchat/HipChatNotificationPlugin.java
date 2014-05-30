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
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
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

/**
 * Sends Rundeck job notification messages to a HipChat room.
 *
 * @author Hayden Bakkum
 */
@Plugin(service= "Notification", name="HipChatNotification")
@PluginDescription(title="HipChat")
public class HipChatNotificationPlugin implements NotificationPlugin {

    private static final String HIPCHAT_API_DEFAULT_BASE_URL = "https://api.hipchat.com";
    private static final String HIPCHAT_API_DEFAULT_VERSION = "v1";
    private static final String HIPCHAT_API_MESSAGE_ROOM_METHOD = "rooms/message";
    private static final String HIPCHAT_API_MESSAGE_ROOM_QUERY = "?auth_token=%s&format=json&message_format=html&room_id=%s&from=%s&message=%s&color=%s";

    private static final String HIPCHAT_MESSAGE_COLOR_GREEN = "green";
    private static final String HIPCHAT_MESSAGE_COLOR_YELLOW = "yellow";
    private static final String HIPCHAT_MESSAGE_COLOR_RED = "red";

    private static final String HIPCHAT_MESSAGE_FROM_NAME = "Rundeck";
    private static final String HIPCHAT_MESSAGE_DEFAULT_TEMPLATE = "hipchat-message.ftl";

    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_SUCCESS = "success";
    private static final String TRIGGER_FAILURE = "failure";

    private static final Map<String, String> TRIGGER_MESSAGE_COLORS = new HashMap<String, String>();
    static {
        TRIGGER_MESSAGE_COLORS.put(TRIGGER_START, HIPCHAT_MESSAGE_COLOR_YELLOW);
        TRIGGER_MESSAGE_COLORS.put(TRIGGER_SUCCESS, HIPCHAT_MESSAGE_COLOR_GREEN);
        TRIGGER_MESSAGE_COLORS.put(TRIGGER_FAILURE, HIPCHAT_MESSAGE_COLOR_RED);
    }

    @PluginProperty(
            title = "Room",
            description = "HipChat room to send notification message to.",
            required = true)
    private String room;

    @PluginProperty(
            title = "HipChat Server Base URL",
            description = "Base URL of HipChat Server",
            required = false,
            defaultValue = HIPCHAT_API_DEFAULT_BASE_URL,
            scope = PropertyScope.Project)
    private String hipchatServerBaseUrl;

    @PluginProperty(
            title = "API Auth Token",
            description = "HipChat API authentication token. Notification level token will do.",
            required = true,
            scope = PropertyScope.Project)
    private String apiAuthToken;

    @PluginProperty(
            title = "Notification Message Template",
            description =
                    "Absolute path to a FreeMarker template that will be used to generate the notification message. " +
                    "If unspecified a default message template will be used.",
            required = false,
            scope = PropertyScope.Project)
    private String messageTemplateLocation;

    /**
     * Sends a message to a HipChat room when a job notification event is raised by Rundeck.
     *
     * @param trigger name of job notification event causing notification
     * @param executionData job execution data
     * @param config plugin configuration
     * @throws HipChatNotificationPluginException when any error occurs sending the HipChat message
     * @return true, if the HipChat API response indicates a message was successfully delivered to a chat room
     */
    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {
        if (!TRIGGER_MESSAGE_COLORS.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        String color = TRIGGER_MESSAGE_COLORS.get(trigger);
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
            // Unfortunately there seems to be no way to obtain a reference to the plugin logger within notification plugins,
            // but throwing an exception will result in its message being logged.
            throw new HipChatNotificationPluginException("Unknown status returned from HipChat API: [" + hipChatResponse.status + "].");
        }
    }

    private String generateMessage(String trigger, Map executionData, Map config) {
        Configuration freeMarkerCfg = new Configuration();
        String templateName;

        if (messageTemplateLocation != null && messageTemplateLocation.length() > 0) {
            File messageTemplateFile = new File(messageTemplateLocation);
            try {
                freeMarkerCfg.setDirectoryForTemplateLoading(messageTemplateFile.getParentFile());
            } catch (IOException ioEx) {
                throw new HipChatNotificationPluginException("Error setting FreeMarker template loading directory: [" + ioEx.getMessage() + "].", ioEx);
            }
            templateName = messageTemplateFile.getName();
        } else {
            freeMarkerCfg.setClassForTemplateLoading(HipChatNotificationPlugin.class, "/templates");
            templateName = HIPCHAT_MESSAGE_DEFAULT_TEMPLATE;
        }

        Map<String, Object> model = new HashMap();
        model.put("trigger", trigger);
        model.put("execution", executionData);
        model.put("config", config);

        StringWriter sw = new StringWriter();
        try {
            Template template = freeMarkerCfg.getTemplate(templateName);
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
        URL requestUrl = toURL(hipchatServerBaseUrl + "/" + HIPCHAT_API_DEFAULT_VERSION + "/" + method + params);

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
            throw new HipChatNotificationPluginException("Failed to obtain HTTP response from [" + hipchatServerBaseUrl + "]: [" + ioEx.getMessage() + "].", ioEx);
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
