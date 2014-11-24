package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

/**
 * @author Hayden Bakkum
 */
public interface HipChatRoomNotifier {

    void sendRoomNotification(String baseURL, String room, String message, String color, String authToken);

    String getSupportedApiVersion();

}
