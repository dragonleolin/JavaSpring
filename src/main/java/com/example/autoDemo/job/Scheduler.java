package com.example.autoDemo.job;

import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.service.KafkaProducerService;
import com.example.autoDemo.service.RedisService;
import com.example.autoDemo.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
public class Scheduler {

    @Autowired
    private StockService stockService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @Value("${stock.list}")
    private List<String> codes;

    // 每小時推播
    //@Scheduled(cron = "${kdj.schedule.cron}")
    // 每天12:15推播
    @Scheduled(cron = "0 15 12 * * ?")
    //@Scheduled(fixedDelay = 120 * 1000, initialDelay= 120 * 1000)
    public void pushDailyStockInfo() {
        List<StockResponse> stockList = stockService.getStockInfo(codes);
        kafkaProducerService.sendStockMessage(stockList);

        LocalDateTime now = LocalDateTime.now();
        String time = now.format(formatter);
        for (String code : codes) {
            stockService.checkAndNotifyKdj(code, true);
        }
        for (StockResponse stock : stockList) {
            redisService.saveToHistory(stock.getCode(), time, stock);
        }
    }
}
