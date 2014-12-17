package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

import com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginException;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpRequestExecutor;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpResponse;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.urlEncode;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Hayden Bakkum
 */
public class HipChatApiVersion2RoomNotifierUnitTest {

    private static final String HIPCHAT_BASE_URL = "";
    private static final String HIPCHAT_ROOM_NAME = "Test Room";
    private static final String HIPCHAT_MESSAGE = "Hello World";
    private static final String HIPCHAT_COLOR = "red";
    private static final String HIPCHAT_AUTH_TOKEN = "abcdef";

    private HttpRequestExecutor requestExecutor;

    private HipChatApiVersion2RoomNotifier roomNotifier;

    @BeforeMethod
    public void setUp() {
        requestExecutor = mock(HttpRequestExecutor.class);

        roomNotifier = new HipChatApiVersion2RoomNotifier(requestExecutor);
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedBaseUrl() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatUrl().startsWith(HIPCHAT_BASE_URL));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedVersion() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatUrl().contains("v2"));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedRoomName() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatUrl().contains(urlEncode(HIPCHAT_ROOM_NAME)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedMessage() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatRequestBody().contains("\"message\":\""+HIPCHAT_MESSAGE+"\""));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedColor() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatRequestBody().contains("\"color\":\""+HIPCHAT_COLOR+"\""));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedAuthToken() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertTrue(captureHipChatUrl().contains("auth_token="+urlEncode(HIPCHAT_AUTH_TOKEN)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedFullUrl() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertEquals(captureHipChatUrl(), HIPCHAT_BASE_URL+"/v2/room/"+urlEncode(HIPCHAT_ROOM_NAME)+"/notification?auth_token="+urlEncode(HIPCHAT_AUTH_TOKEN));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedFullRequestBody() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );

        assertEquals(captureHipChatRequestBody(), "{\"message\":\""+HIPCHAT_MESSAGE+"\",\"color\":\""+HIPCHAT_COLOR+"\",\"message_format\":\"html\"}");
    }

    @Test(expectedExceptions = HipChatNotificationPluginException.class)
    public void testHipChatExceptionThrownWhenHipChatAPIReturnsError() {
        final HttpResponse httpResponse = createHipChatHttpResponse(false);
        when(requestExecutor.execute(anyString(), anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN
        );
    }

    private String captureHipChatUrl() {
        return captureRequestExecutorArgs().get(0);
    }

    private String captureHipChatRequestBody() {
        return captureRequestExecutorArgs().get(1);
    }

    private List<String> captureRequestExecutorArgs() {
        final ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(requestExecutor).execute(argument.capture(), argument.capture());

        return argument.getAllValues();
    }

    private HttpResponse createHipChatHttpResponse(final boolean didSend) {
        final int responseCode = didSend ? HttpResponse.STATUS__NO_CONTENT : 400;

        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getResponseCode()).thenReturn(responseCode);

        return httpResponse;
    }

}
