package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

/**
 * @author Hayden Bakkum
 */
public interface HipChatRoomNotifier {

    boolean sendRoomNotification(String baseURL, String room, String message, String color, String authToken);

    String getSupportedApiVersion();

}
