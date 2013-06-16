package com.github.rundeck.plugin.hipchat;

public class HipChatNotificationPluginException extends RuntimeException {

    public HipChatNotificationPluginException(String message) {
        super(message);
    }

    public HipChatNotificationPluginException(String message, Throwable cause) {
        super(message, cause);
    }

}
