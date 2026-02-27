package com.fran.dev.potjera.serverbackend.configuration;

import com.fran.dev.potjera.serverbackend.controllers.websocket.GameSessionWebSocketController;
import com.fran.dev.potjera.serverbackend.interceptors.JwtHandshakeHandler;
import com.fran.dev.potjera.serverbackend.interceptors.JwtStompInterceptor;
import com.fran.dev.potjera.serverbackend.services.CustomUserDetailsService;
import com.fran.dev.potjera.serverbackend.utilities.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final JwtStompInterceptor jwtStompInterceptor;

    public WebSocketConfig(JwtUtil jwtUtil, JwtStompInterceptor jwtStompInterceptor) {
        this.jwtUtil = jwtUtil;
        this.jwtStompInterceptor = jwtStompInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Server -> Client (subscriptions)
        registry.enableSimpleBroker("/topic", "/queue");

        // Client -> Server
        registry.setApplicationDestinationPrefixes("/app");

        // Required for /user/queue
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                //.setHandshakeHandler(new JwtHandshakeHandler(jwtUtil))
                .withSockJS(); // needed for Android Stomp client
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompInterceptor);
    }
}
