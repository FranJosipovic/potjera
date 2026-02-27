package com.fran.dev.potjera.serverbackend.interceptors;

import com.fran.dev.potjera.serverbackend.utilities.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtStompInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public JwtStompInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.get(0).replace("Bearer ", "");

                if (jwtUtil.isTokenValid(token)) {
                    Long userId = jwtUtil.extractUserId(token);

                    User principal = new User(userId.toString(), "", List.of());

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());

                    accessor.setUser(auth);
                }
            }
        }

        return message;
    }
}
