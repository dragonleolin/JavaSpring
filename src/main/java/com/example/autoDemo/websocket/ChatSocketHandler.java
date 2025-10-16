package com.example.autoDemo.websocket;

import com.example.autoDemo.data.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.autoDemo.service.AIChatService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSocketHandler implements WebSocketHandler {

    private final AIChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("✅ WebSocket 連線建立: {}", session.getId());
        sessionMap.put(session.getId(), session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String content = (String) msg.get("content");

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);

            // 使用者的原始訊息
            ChatMessage userMessage = new ChatMessage("user", content, formattedDateTime);
            log.info("📩 使用者訊息: {}", content);

            // 呼叫 AI
            String aiReply = aiChatService.generateAIReply(content);

            ChatMessage aiMessage = new ChatMessage("AI", aiReply, formattedDateTime);

            // 回傳給前端
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(aiMessage)));

        } catch (Exception e) {
            log.error("❌ WebSocket handleMessage error", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("⚠️ WebSocket 傳輸錯誤", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        log.info("🔌 WebSocket 連線關閉: {}", session.getId());
        sessionMap.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

