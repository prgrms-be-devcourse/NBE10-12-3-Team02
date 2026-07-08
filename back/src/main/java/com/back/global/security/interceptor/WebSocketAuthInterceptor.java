package com.back.global.security.interceptor;

import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.jwt.payload.AccessTokenPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring("Bearer ".length()).trim();

                try {
                    AccessTokenPayload payload = jwtTokenProvider.parseAccessToken(token);
                    accessor.setUser(new StompPrincipal(payload.userId().toString()));
                    log.info("WebSocket 인증 성공: userId={}", payload.userId());
                } catch (Exception e) {
                    log.warn("WebSocket 인증 실패: {}", e.getMessage());
                }
            }
        }
        return message;
    }

    public record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
