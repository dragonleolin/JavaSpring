package com.example.autoDemo.controller;

import com.example.autoDemo.data.KdjData;
import com.example.autoDemo.data.StockRequest;
import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.service.KafkaProducerService;
import com.example.autoDemo.service.RedisService;
import com.example.autoDemo.service.StockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "http://localhost:5173")  // 前端 Vue 預設 Port 是 5173
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

    @GetMapping("/count/{code}")
    public ResponseEntity<String> getQueryCount(@PathVariable String code) {
        long count = redisService.getQueryCount(code);
        return ResponseEntity.ok("股票代號 " + code + " 被查詢次數為：" + count);
    }
    // URL: http://localhost:8082/stock/chart/0050?from=20250624&to=2025072
    @GetMapping(value = "/chart/{code}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getChartImage(
            @PathVariable String code,
            @RequestParam String from,
            @RequestParam String to
    ) {
        try {
            byte[] chartImage = redisService.generateChartAndSendToTelegram(code, from, to);
            if (chartImage == null || chartImage.length == 0) {
                System.out.println("chartImage: null");
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(chartImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/chart/query-count")
    public ResponseEntity<byte[]> getQueryCountChart() {
        byte[] image = redisService.generatePieChartAndSendToTelegram();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    // 取KD值
    @GetMapping("/kdj/{symbol}")
    public ResponseEntity<String> getLatestKdj(
            @PathVariable String symbol,
            @RequestParam String from,
            @RequestParam String to) {

        KdjData latest = stockService.getLatestKdj(symbol, from, to);
        System.out.println("latest:"+ latest);
        if (latest != null) {
            String msg = String.format("代碼：%s\n日期：%s\nK值：%.2f\nD值：%.2f",
                    symbol, latest.getDate(), latest.getK(), latest.getD());
            return ResponseEntity.ok(msg);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("查無資料");
        }
    }

    @GetMapping("/sendKdj/{symbol}")
    public ResponseEntity<String> getLatestKdj(
            @PathVariable String symbol) throws JsonProcessingException {
        stockService.checkAndNotifyKdj(symbol, false);
        return ResponseEntity.ok("傳送成功");
    }

}

