package com.example.autoDemo.service;

import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.util.ChartUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Autowired
    @Qualifier("countRedisTemplate")
    private RedisTemplate<String, Object> countRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    // Redis 中存所有股票的 Hash Key 名稱
    private final String redisKey = "stock:data";

    public void saveToCache(String code, StockResponse response) {
        String key = "stock:" + code;
        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(10));
    }

    public void saveToHistory(String code, String time, StockResponse response) {
        String key = String.format("stock:%s:%s", code, time.replace(":", "").replace(" ", "-"));
        redisTemplate.opsForValue().set(key, response);
        // 更新統計次數
        countRedisTemplate.opsForHash().increment("stock:count", code, 1);
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

        // 更新統計次數
        countRedisTemplate.opsForHash().increment("stock:count", code, 1);
        return result;
    }

    public long getQueryCount(String code) {
        if (countRedisTemplate.opsForHash().get("stock:count", code) == null) {
            countRedisTemplate.opsForHash().put("stock:count", code, 0);
        }
        Object count = countRedisTemplate.opsForHash().get("stock:count", code);
        return count == null ? 0 : Long.parseLong(count.toString());
    }

    public Map<String, Long> getQueryCountMap() {
        Map<Object, Object> entries = countRedisTemplate.opsForHash().entries("stock:count");
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put(String.valueOf(entry.getKey()), Long.parseLong(String.valueOf(entry.getValue())));
        }
        return result;
    }

    public byte[] generatePieChartAndSendToTelegram() {
        Map<String, Long> data = getQueryCountMap();
        if (data.isEmpty()) return null;

        byte[] image = ChartUtil.generatePieChart(data, "查詢次數統計");
        kafkaProducerService.sendPhoto(image, "查詢次數統計圖");

        return  image;
    }

    public Optional<Map.Entry<String, StockResponse>> getLatestByCode(String code) {
        Set<String> keys = redisTemplate.keys("stock:" + code + ":*");
        if (keys == null) return Optional.empty();
        // 增加查詢次數
        redisTemplate.opsForHash().increment("stock:count", code, 1);
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
    public byte[] generateChartAndSendToTelegram(String code, String fromDate, String toDate) throws IOException {
        List<StockResponse> data = getByCode(code).stream()
                .filter(s -> {
                    String date = s.getMarketTime().substring(0, 8);
                    return date.compareTo(fromDate) >= 0 && date.compareTo(toDate) <= 0;
                })
                .sorted(Comparator.comparing(StockResponse::getMarketTime))
                .collect(Collectors.toList());
        byte[] image = new byte[0];
        if (!data.isEmpty()) {
            image = chartUtil.generateLineChartImage(code, data);
            kafkaProducerService.sendPhoto(image, "股票價格走勢圖");
        }
        return image;
    }

}
