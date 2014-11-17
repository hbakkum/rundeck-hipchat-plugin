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
import com.hbakkum.rundeck.plugins.hipchat.roomnotifier.HipChatRoomNotifier;
import com.hbakkum.rundeck.plugins.hipchat.roomnotifier.HipChatRoomNotifierFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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

    private static final String HIPCHAT_MESSAGE_COLOR_GREEN = "green";
    private static final String HIPCHAT_MESSAGE_COLOR_YELLOW = "yellow";
    private static final String HIPCHAT_MESSAGE_COLOR_RED = "red";

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
            title = "HipChat API Version",
            description = "HipChat API version to use ",
            required = false,
            defaultValue = HIPCHAT_API_DEFAULT_VERSION,
            scope = PropertyScope.Project)
    private String apiVersion;

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

        final HipChatRoomNotifier hipChatRoomNotifier = HipChatRoomNotifierFactory.get(apiVersion);

        final String color = TRIGGER_MESSAGE_COLORS.get(trigger);
        final String message = generateMessage(trigger, executionData, config);

        return hipChatRoomNotifier.sendRoomNotification(hipchatServerBaseUrl, room, message, color, apiAuthToken);
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

}
