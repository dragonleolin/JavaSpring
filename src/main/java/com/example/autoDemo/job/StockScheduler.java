package com.example.autoDemo.job;

import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.service.KafkaProducerService;
import com.example.autoDemo.service.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class StockScheduler {

    private final StockService stockService;
    private final KafkaProducerService kafkaProducerService;

    private final Set<String> codes = new HashSet<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StockScheduler(StockService stockService,
                          KafkaProducerService kafkaProducerService,
                          @Value("${stock.list}") List<String> defaultCodes) {
        this.stockService = stockService;
        this.kafkaProducerService = kafkaProducerService;
        this.codes.addAll(defaultCodes);
    }

    public void addCode(String code) {
        codes.add(code);
    }

    public Set<String> getCodes() {
        return codes;
    }

    public boolean removeCode(String code) {
        return codes.remove(code);
    }

    @Scheduled(cron = "0 15 12 * * ?", zone = "Asia/Taipei") // 每天中午 12:15 台灣時間
    public void pushDailyStockInfo() {
        if (codes.isEmpty()) return;

        List<StockResponse> stockList = stockService.getStockInfo(new ArrayList<>(codes));
        kafkaProducerService.sendStockMessage(stockList);

        for (String code : codes) {
            stockService.checkAndNotifyKdj(code, true);
        }
    }
}
