package com.example.autoDemo.controller;

import com.example.autoDemo.data.StockRequest;
import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.service.KafkaProducerService;
import com.example.autoDemo.service.RedisService;
import com.example.autoDemo.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisService redisService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @PostMapping
    public List<StockResponse> getStocks(@RequestBody StockRequest request) {
        List<String> codes = request.getCodes();
        List<StockResponse> result = new ArrayList<>();
        for (String code : codes) {
            result = stockService.getStockInfo(codes);
        }
        //kafkaProducerService.sendStockMessage(result); //傳送給Telegram
        return result;
    }

    // 查詢所有 Redis 快取
    @GetMapping("/cache")
    public List<StockResponse> getAllCache() {
        return redisService.getSortedStocksFromRedis();
    }

    // 查詢指定代碼所有快取紀錄
    @GetMapping("/cache/{code}")
    public List<StockResponse> getCacheByCode(@PathVariable String code) {
        List<StockResponse> data = redisService.getByCode(code);
        return data;
    }

    // 查詢最新一筆快取（依代碼）
    @GetMapping("/cache/{code}/latest")
    public ResponseEntity<Map.Entry<String, StockResponse>> getLatestCache(@PathVariable String code) {
        Optional<Map.Entry<String, StockResponse>> latest = redisService.getLatestByCode(code);
        return latest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 刪除指定快取
    @DeleteMapping("/cache/{code}/{time}")
    public ResponseEntity<String> deleteCache(@PathVariable String code, @PathVariable String time) {
        redisService.delete(code, time);
        return ResponseEntity.ok("Deleted cache for " + code + " at " + time);
    }

    // 清空所有快取
    @DeleteMapping("/cache/clear")
    public ResponseEntity<String> clearAllCache() {
        redisService.clearAll();
        return ResponseEntity.ok("All Redis cache cleared.");
    }

    // URL: http://localhost:8082/stock/chart/0050?from=20250624&to=2025072
    @GetMapping("/chart/{code}")
    public ResponseEntity<String> getChart(
            @PathVariable String code,
            @RequestParam String from,
            @RequestParam String to
    ) throws IOException {
        redisService.generateChartAndSendToTelegram(code, from, to);
        return ResponseEntity.ok("Chart generated and sent to Telegram for " + code);
    }

}

