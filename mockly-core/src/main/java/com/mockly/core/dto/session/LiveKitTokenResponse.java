package com.mockly.core.dto.session;

/**
 * Response containing LiveKit access token and room information.
 */
public record LiveKitTokenResponse(
        /**
         * LiveKit access token for WebRTC connection.
         */
        String token,

        /**
         * LiveKit room ID.
         */
        String roomId,

        /**
         * LiveKit server URL.
         */
        String url
) {}

