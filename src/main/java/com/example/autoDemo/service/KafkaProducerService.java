package com.example.autoDemo.service;

import com.example.autoDemo.data.StockInfo;
import com.example.autoDemo.data.StockResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, StockInfo> kafkaTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

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

    public void sendStockInfo(StockInfo info) {
        kafkaTemplate.send("stock-topic", info);
    }
}

