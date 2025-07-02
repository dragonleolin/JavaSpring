package com.example.autoDemo.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class N8nService {

    private final String N8N_WEBHOOK_URL = "http://n8n:5678/webhook/myworkflow";

    public void triggerN8nWorkflow(String payload) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(N8N_WEBHOOK_URL, request, String.class);
            System.out.println("✅ 已發送至 n8n webhook");
        } catch (Exception e) {
            System.err.println("❌ 發送 n8n webhook 失敗: " + e.getMessage());
        }
    }
}
