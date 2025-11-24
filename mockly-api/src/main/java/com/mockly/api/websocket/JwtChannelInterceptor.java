package com.mockly.api.websocket;

import com.mockly.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Interceptor for WebSocket JWT authentication.
 * Validates JWT token from STOMP headers and sets authentication in security context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_HEADER = "token";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract token from headers
            String token = extractToken(accessor);
            
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                try {
                    String userId = jwtTokenProvider.getUserIdFromToken(token).toString();
                    String role = jwtTokenProvider.getRoleFromToken(token);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );

                    // Set authentication in accessor for WebSocket session
                    accessor.setUser(authentication);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("WebSocket connection authenticated for user: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to authenticate WebSocket connection", e);
                    throw new SecurityException("Invalid JWT token for WebSocket connection", e);
                }
            } else {
                log.warn("WebSocket connection rejected: invalid or missing JWT token");
                throw new SecurityException("WebSocket connection requires valid JWT token");
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     * Supports both "Authorization: Bearer <token>" and "token: <token>" formats.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first
        List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }

        // Try token header
        List<String> tokenHeaders = accessor.getNativeHeader(TOKEN_HEADER);
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}

