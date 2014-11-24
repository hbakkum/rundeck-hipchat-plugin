package com.hbakkum.rundeck.plugins.hipchat;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and manages API Auth tokens for HipChat rooms. Loads auth token for a room from a string representation:
 *
 * ${room_name_or_id}:${api_auth_token}
 *
 * e.g. 12345:TT0Xj1dPMP4rOKbza4hqP2GNEgbtv9BZWisDavy0
 *
 * Comma separate to specify tokens for multiple rooms:
 *
 * 12345:TT0Xj1dPMP4rOKbza4hqP2GNEgbtv9BZWisDavy0, 2468:P2GNEgbtv9BZWisDavy0TT0Xj1dPMP4rOKbza4hq
 *
 * If the ${room_name_or_id} component is dropped, then the token will be used as the default in the case where no token is found for a room:
 *
 * 12345:TT0Xj1dPMP4rOKbza4hqP2GNEgbtv9BZWisDavy0, 2468:P2GNEgbtv9BZWisDavy0TT0Xj1dPMP4rOKbza4hq, WisDavy0TT0Xj1dPdPMP4rOKbza4hqP2GNEgbt
 *                                                                                                                      ^
 *                                                                                                                 default token
 *
 * For HipChat API v1 use, a single notification level token will work for every room and thus only a single default token needs to be specified.
 * For HipChat API v2 use, a 'room notification' token may need to be generated for each target room
 *
 * @author hbakkum
 */
public class HipChatApiAuthTokenManager {

    private final Map<String, String> roomApiAuthTokenAssociations = new HashMap<String, String>();

    private String defaultApiAuthToken;

    public HipChatApiAuthTokenManager(final String apiAuthTokenData) {
        load(apiAuthTokenData);
    }

    public String getApiAuthTokenForRoom(final String room) {
        if (roomApiAuthTokenAssociations.containsKey(room)) {
            return roomApiAuthTokenAssociations.get(room);
        } else {
            return defaultApiAuthToken;
        }
    }

    private void load(final String apiAuthTokenData) {
        final String[] apiAuthTokens = apiAuthTokenData.trim().split("\\s*,\\s*");
        for (final String apiAuthToken : apiAuthTokens) {
            final String[] apiAuthTokenParts = apiAuthToken.split(":");
            if (apiAuthTokenParts.length == 2) {
                roomApiAuthTokenAssociations.put(apiAuthTokenParts[0], apiAuthTokenParts[1]);
            } else {
                defaultApiAuthToken = apiAuthTokenParts[0];
            }
        }
    }

}
