package com.example.autoDemo.service;

import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.util.ChartUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    @Autowired
    KafkaProducerService kafkaProducerService;
    @Autowired
    ChartUtil chartUtil;
    @Autowired
    private RedisTemplate<String, StockResponse> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveToCache(String code, StockResponse response) {
        String key = "stock:" + code;
        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(10));
    }

    public void saveToHistory(String code, String time, StockResponse response) {
        String key = String.format("stock:%s:%s", code, time.replace(":", "").replace(" ", "-"));
        redisTemplate.opsForValue().set(key, response);
    }

    public StockResponse getFromCache(String code) {
        String key = "stock:" + code;
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof StockResponse) {
            return (StockResponse) val;
        }
        return null;
    }

    public List<StockResponse> getByCode(String code) {
        Set<String> keys = redisTemplate.keys("stock:" + code + ":*");
        if (keys == null) return new ArrayList<>();
        List<StockResponse> result = new ArrayList<>();
        for (String key : keys) {
            Object val = redisTemplate.opsForValue().get(key);
            if (val instanceof StockResponse) {
                result.add((StockResponse) val);
            }
        }
        result.sort(Comparator.comparing(StockResponse::getMarketTime).reversed());
        return result;
    }

    public Optional<Map.Entry<String, StockResponse>> getLatestByCode(String code) {
        Set<String> keys = redisTemplate.keys("stock:" + code + ":*");
        if (keys == null) return Optional.empty();
        return keys.stream()
                .map(k -> Map.entry(k, (StockResponse) redisTemplate.opsForValue().get(k)))
                .filter(e -> e.getValue() != null)
                .max(Comparator.comparing(e -> e.getValue().getMarketTime()));
    }

    public List<StockResponse> getSortedStocksFromRedis() {
        Set<String> keys = redisTemplate.keys("stock:*:*");
        if (keys == null) return new ArrayList<>();
        return keys.stream()
                .map(k -> (StockResponse) redisTemplate.opsForValue().get(k))
                .filter( value -> value.getMarketTime() != null && !value.getMarketTime().equals("-"))
                .sorted(Comparator
                // 時間時間倒序（新到舊）
                .comparing(StockResponse::getMarketTime, Comparator.nullsLast(Comparator.reverseOrder()))
                // 若時間一樣則比股票代碼
                .thenComparing(StockResponse::getCode)
                ).collect(Collectors.toList());
    }

    public void delete(String code, String time) {
        String key = String.format("stock:%s:%s", code, time);
        redisTemplate.delete(key);
    }

    public void clearAll() {
        Set<String> keys = redisTemplate.keys("stock:*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }
    public void generateChartAndSendToTelegram(String code, String fromDate, String toDate) throws IOException {
        List<StockResponse> data = getByCode(code).stream()
                .filter(s -> {
                    String date = s.getMarketTime().substring(0, 8);
                    return date.compareTo(fromDate) >= 0 && date.compareTo(toDate) <= 0;
                })
                .sorted(Comparator.comparing(StockResponse::getMarketTime))
                .collect(Collectors.toList());

        if (!data.isEmpty()) {
            byte[] image = new byte[0];
            image = chartUtil.generateLineChartImage(code, data);
            kafkaProducerService.sendPhoto(image);
        }
    }
}
