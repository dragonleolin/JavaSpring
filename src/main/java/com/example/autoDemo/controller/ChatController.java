package com.example.autoDemo.controller;

import com.example.autoDemo.data.ChatMessageRequest;
import com.example.autoDemo.service.AIChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final AIChatService aiChatService;

    @PostMapping("/send")
    public String sendMessage(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        return aiChatService.generateAIReply(content);
    }
}
