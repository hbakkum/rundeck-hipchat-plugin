package com.hbakkum.rundeck.plugins.hipchat.roomnotifier;

import com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginException;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpRequestExecutor;
import com.hbakkum.rundeck.plugins.hipchat.http.HttpResponse;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.hbakkum.rundeck.plugins.hipchat.HipChatNotificationPluginUtils.urlEncode;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Hayden Bakkum
 */
public class HipChatApiVersion1RoomNotifierUnitTest {

    private static final String HIPCHAT_BASE_URL = "";
    private static final String HIPCHAT_ROOM_NAME = "Test Room";
    private static final String HIPCHAT_MESSAGE = "Hello World";
    private static final String HIPCHAT_COLOR = "red";
    private static final String HIPCHAT_AUTH_TOKEN = "abcdef";

    private HttpRequestExecutor requestExecutor;

    private HipChatApiVersion1RoomNotifier roomNotifier;

    @BeforeMethod
    public void setUp() {
        requestExecutor = mock(HttpRequestExecutor.class);

        roomNotifier = new HipChatApiVersion1RoomNotifier(requestExecutor);
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedBaseUrl() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().startsWith(HIPCHAT_BASE_URL));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedVersion() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().contains("v1"));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedRoomName() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().contains("room_id="+urlEncode(HIPCHAT_ROOM_NAME)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedMessage() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().contains("message="+urlEncode(HIPCHAT_MESSAGE)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedColor() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().contains("color="+urlEncode(HIPCHAT_COLOR)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedAuthToken() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertTrue(captureHipChatUrl().contains("auth_token="+urlEncode(HIPCHAT_AUTH_TOKEN)));
    }

    @Test
    public void testSendRoomNotificationUrlHasExpectedFullUrl() {
        final HttpResponse httpResponse = createHipChatHttpResponse(true);
        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );

        assertEquals(captureHipChatUrl(), HIPCHAT_BASE_URL+"/v1/rooms/message?auth_token="+urlEncode(HIPCHAT_AUTH_TOKEN)+"&format=json&message_format=html&room_id="+urlEncode(HIPCHAT_ROOM_NAME)+"&from=Rundeck&message="+urlEncode(HIPCHAT_MESSAGE)+"&color="+urlEncode(HIPCHAT_COLOR)+"&notify=1");
    }

    @Test(expectedExceptions = HipChatNotificationPluginException.class)
    public void testHipChatExceptionThrownWhenHipChatAPIReturnsError() {
        final HttpResponse httpResponse = createHipChatHttpResponse(false);

        when(requestExecutor.execute(anyString())).thenReturn(httpResponse);

        roomNotifier.sendRoomNotification(
                HIPCHAT_BASE_URL,
                HIPCHAT_ROOM_NAME,
                HIPCHAT_MESSAGE,
                HIPCHAT_COLOR,
                HIPCHAT_AUTH_TOKEN,
                true
        );
    }

    private String captureHipChatUrl() {
        final ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(requestExecutor).execute(argument.capture());

        return argument.getValue();
    }

    private HttpResponse createHipChatHttpResponse(final boolean didSend) {
        final String status = didSend ? "sent" : "error";

        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getContentType()).thenReturn(HttpResponse.CONTENT_TYPE__JSON);
        when(httpResponse.getResponseBody()).thenReturn("{ \"status\": \""+status+"\" }");

        return httpResponse;
    }

}
