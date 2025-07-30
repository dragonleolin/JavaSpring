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
    private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            String content = (String) msg.get("content");
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(content);
            chatMessage.setSentAt(formattedDateTime);
            // 呼叫 AI API（可封裝成 chatService.generateAIReply(...)）
            String aiReply = aiChatService.generateAIReply(content);

            // 包裝 AI 回覆訊息
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setContent(aiReply);
            aiMessage.setSentAt(formattedDateTime);

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(aiMessage)));

            return; // 不往下執行，一般用戶之間才會傳送到 receiver

        } catch (Exception e) {
            log.error("WebSocket handleMessage error", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

