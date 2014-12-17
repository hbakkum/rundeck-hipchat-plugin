package com.hbakkum.rundeck.plugins.hipchat;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HipChatApiAuthTokenManagerUnitTest {

    private static final String MULTIPLE_ROOM_AUTH_TOKENS_AND_DEFAULT = "1111:atoken, 2222:anothertoken, defaulttoken";

    private HipChatApiAuthTokenManager hipChatApiAuthTokenManager;

    @Test
    public void testRoomTokenIsReturnedWhenOneExists() {
        hipChatApiAuthTokenManager = new HipChatApiAuthTokenManager(MULTIPLE_ROOM_AUTH_TOKENS_AND_DEFAULT);

        assertEquals(hipChatApiAuthTokenManager.getApiAuthTokenForRoom("2222"), "anothertoken");
    }

    @Test
    public void testDefaultTokenIsReturnedWhenNoTokenExistsforRoom() {
        hipChatApiAuthTokenManager = new HipChatApiAuthTokenManager(MULTIPLE_ROOM_AUTH_TOKENS_AND_DEFAULT);

        assertEquals(hipChatApiAuthTokenManager.getApiAuthTokenForRoom("3333"), "defaulttoken");
    }

    @Test
    public void testDefaultTokenIsReturnedWhenOnlyDefaultTokenIsSpecified() {
        hipChatApiAuthTokenManager = new HipChatApiAuthTokenManager("defaulttoken");

        assertEquals(hipChatApiAuthTokenManager.getApiAuthTokenForRoom("1111"), "defaulttoken");
    }

}
