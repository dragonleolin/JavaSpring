package com.example.autoDemo.controller;

import com.example.autoDemo.data.ChatMessageRequest;
import com.example.autoDemo.service.AIChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final AIChatService aiChatService;

    @PostMapping("/send")
    public ResponseEntity<?> send (@RequestBody ChatMessageRequest request, HttpServletRequest httpServletRequest){
        //chatService.sendMessage(httpServletRequest, request);
        return ResponseEntity.ok("已送出訊息");
    }
}
