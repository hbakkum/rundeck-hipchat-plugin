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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(HipChatNotificationPlugin.class);

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
            title = "Room(s)",
            description = "HipChat room name or ID (ID is recommended) to send notification message to. To specify multiple rooms, separate with a comma",
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
            title = "API Auth Token(s)",
            description = "HipChat API authentication token(s). For HipChat API v1, a single notification level token will do. For HipChat API v2, a token per room may be required - see user guide for more information",
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

    @PluginProperty(
            title = "Proxy Host",
            description = "Proxy host to use when communicating to the HipChat API.",
            required = false,
            defaultValue = "",
            scope = PropertyScope.Project)
    private String proxyHost;

    @PluginProperty(
            title = "Proxy Port",
            description = "Proxy port to use when communicating to the HipChat API.",
            required = false,
            defaultValue = "",
            scope = PropertyScope.Project)
    private String proxyPort;

    /**
     * Sends a message to a HipChat room when a job notification event is raised by Rundeck.
     *
     * @param trigger name of job notification event causing notification
     * @param executionData job execution data
     * @param config plugin configuration
     * @throws HipChatNotificationPluginException when any error occurs sending the HipChat message
     * @return true, if all HipChat notifications were successfully sent to each room
     */
    @Override
    public boolean postNotification(final String trigger, final Map executionData, final Map config) {
        if (!TRIGGER_MESSAGE_COLORS.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        final HipChatRoomNotifier hipChatRoomNotifier = HipChatRoomNotifierFactory.get(apiVersion, proxyHost, proxyPort);
        final HipChatApiAuthTokenManager hipChatApiAuthTokenManager = new HipChatApiAuthTokenManager(apiAuthToken);
        final HipChatNotificationMessageGenerator hipChatNotificationMessageGenerator = new HipChatNotificationMessageGenerator();

        final String color = TRIGGER_MESSAGE_COLORS.get(trigger);
        final String message = hipChatNotificationMessageGenerator.generateMessage(messageTemplateLocation, HIPCHAT_MESSAGE_DEFAULT_TEMPLATE, trigger, executionData, config);

        return sendRoomNotifications(hipChatRoomNotifier, hipChatApiAuthTokenManager, message, color);
    }

    private boolean sendRoomNotifications(
            final HipChatRoomNotifier hipChatRoomNotifier,
            final HipChatApiAuthTokenManager hipChatApiAuthTokenManager,
            final String message,
            final String color) {
        boolean didAllNotificationsSendSuccessfully = true;

        final String[] rooms = this.room.trim().split("\\s*,\\s*");
        for (final String room : rooms) {
            final String apiAuthTokenForRoom = hipChatApiAuthTokenManager.getApiAuthTokenForRoom(room);
            if (apiAuthTokenForRoom == null || apiAuthTokenForRoom.isEmpty()) {
                LOG.error("Cannot send notification to room [{}] as no API Auth Token found for this room.", room);
                continue;
            }

            try {
                hipChatRoomNotifier.sendRoomNotification(hipchatServerBaseUrl, room, message, color, apiAuthTokenForRoom);

            } catch (Exception ex) {
                LOG.error("Error sending HipChat notification to room: [{}]", room, ex);
                didAllNotificationsSendSuccessfully = false;
            }
        }

        return didAllNotificationsSendSuccessfully;
    }

}
