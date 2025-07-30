package com.example.autoDemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIChatService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate;

    public String generateAIReply(String userMessage) {
        String url = "https://openrouter.ai/api/v1/chat/completions";
        String model = "openai/gpt-4o";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "你是一位專業的股票市場投資人，請用親切的語氣回覆用戶"),
                Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        headers.set("HTTP-Referer", "http://localhost:8082");
        headers.set("X-Title", "startMatch-AI");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                System.out.println("message:"+ message);
                return (String) message.get("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 回覆失敗: " + e.getMessage();
        }

        return "AI 沒有給出回應";
    }
}
