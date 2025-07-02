package com.example.autoDemo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerWorkflow(@RequestBody String payload) {
        // 假設這裡會呼叫 n8n 的 webhook URL
        System.out.println("收到資料: " + payload);
        return ResponseEntity.ok("Webhook received");
    }
}
