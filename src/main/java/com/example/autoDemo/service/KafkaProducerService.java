package com.example.autoDemo.service;

import com.example.autoDemo.data.StockResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KafkaProducerService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.chat.id}")
    private String chatId;
    @Value("${line.message.token}")
    private String lineToken;
    @Value("${line.message.userId}")
    private String userId;

    public void sendStockMessage(List<StockResponse> stockList) {
        StringBuffer message = new StringBuffer("今日股價資訊\n\n");
        for (StockResponse stock : stockList) {
            message.append(String.format("代號：%s\n名稱：%s\n價格：%s\n時間：%s\n\n",
                    stock.getCode(), stock.getName(), stock.getPrice(), stock.getMarketTime()));
        }

        String telegramUrl = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                botToken, chatId, message.toString());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(telegramUrl, String.class);
    }

    public void sendPhoto(byte[] imageBytes, String caption) {
        String url = String.format("https://api.telegram.org/bot%s/sendPhoto", botToken);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("caption", caption);
        body.add("photo", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "chart.png";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        new RestTemplate().postForEntity(url, request, String.class);
    }


    public void sendLineMessage(String message) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // 建立文字訊息物件
        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("type", "text");
        textMessage.put("text", message);

        // 包裝為 messages 陣列
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(textMessage);

        // 包裝整個請求 body
        Map<String, Object> body = new HashMap<>();
        body.put("to", userId);          // 對方 userId
        body.put("messages", messages);  // 正確格式的 messages

        // 設定 Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(lineToken); // LINE Messaging API 的 Channel Access Token

        // 建立 HTTP 請求
        HttpEntity<String> requestEntity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.line.me/v2/bot/message/push",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            System.out.println("LINE API 回應: " + response.getStatusCode());
            System.out.println("內容: " + response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("發送失敗: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        }
    }


    //public void sendStockInfo(StockInfo info) {
    //    kafkaTemplate.send("stock-topic", info);
    //}
}

