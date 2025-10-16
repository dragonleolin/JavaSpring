package com.example.autoDemo.config;

import com.example.autoDemo.websocket.ChatSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatSocketHandler chatSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 註冊 WebSocket endpoint
        registry.addHandler(chatSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}