package com.example.autoDemo.service;

import com.example.autoDemo.data.FugleKdjResponse;
import com.example.autoDemo.data.KdjData;
import com.example.autoDemo.data.StockResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    @Value("${fugle.apikey}")
    private String fugleToken;
    @Autowired
    RedisService redisService;
    @Autowired
    KafkaProducerService kafkaProducerService;
    @Autowired
    private RedisTemplate<String, StockResponse> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    public List<StockResponse> getStockInfo(List<String> stockCodes) {
        List<StockResponse> responseList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        for (String code : stockCodes) {
            StockResponse cached = redisService.getFromCache(code);
            if (cached != null) {
                responseList.add(cached);
                continue;
            }
            try {
                String url = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp?ex_ch=tse_" + code + ".tw";
                String response = restTemplate.getForObject(url, String.class);

                JSONObject json = new JSONObject(response);
                JSONObject stock = json.getJSONArray("msgArray").getJSONObject(0);
                LocalDateTime now = LocalDateTime.now();
                String formattedTime = now.format(DATE_TIME_FORMATTER);

                String name = stock.getString("n");
                String price = stock.getString("z");
                String time = stock.getString("t");
                if ("-".equals(price)) {
                    price = stock.getString("h"); // 若沒有試算參考成交量，取當日最高價
                }
                StockResponse stockResponse = new StockResponse();
                stockResponse.setCode(code);
                stockResponse.setName(name);
                stockResponse.setPrice(price);
                stockResponse.setMarketTime(formattedTime);

                // 寫入 Redis 快取 (10 分鐘)
                redisService.saveToCache(code, stockResponse);

                // 寫入 Redis 歷史資料 (不設定 TTL)
                redisService.saveToHistory(code, formattedTime, stockResponse);

                responseList.add(stockResponse);
            } catch (Exception e) {
                responseList.add(new StockResponse(code, "查詢失敗", "-", "-"));
            }
        }

        return responseList;
    }

    public KdjData getLatestKdj(String symbol, String from, String to) {
        //System.out.println("start getLatestKdj");
        //System.out.println("fugleToken:"+ fugleToken);
        String url = String.format(
                "https://api.fugle.tw/marketdata/v1.0/stock/technical/kdj/%s?from=%s&to=%s&timeframe=D&rPeriod=9&kPeriod=3&dPeriod=3",
                symbol, from, to);
        // System.out.println("url:"+ url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", fugleToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<FugleKdjResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, FugleKdjResponse.class);
        //System.out.println("getLatestKdj response:"+ response);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().getData().isEmpty()) {
            // 取最後一筆
            return response.getBody().getData().get(response.getBody().getData().size() - 1);
        }

        return null;
    }

    public void checkAndNotifyKdj(String stockNo, boolean mayBuy) {
        LocalDate yesterday = getPreviousWorkday(LocalDate.now());
        System.out.println("最近的工作日是: " + yesterday);
        String today = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        KdjData kd = getLatestKdj(stockNo, today, today);
        System.out.println("kd:"+ kd);
        if (kd == null) return;
        if (mayBuy){
            // 發送通知
            if (kd.getK() < 25) {
                try {
                    kafkaProducerService.sendLineMessage(String.format("⚠️ KD警示\n股票：%s\nK=%.2f, D=%.2f\n時間：%s", stockNo, kd.getK(), kd.getD(), kd.getDate()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try {
                kafkaProducerService.sendLineMessage(String.format("⚠️ KD警示\n股票：%s\nK=%.2f, D=%.2f\n時間：%s", stockNo, kd.getK(), kd.getD(), kd.getDate()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static LocalDate getPreviousWorkday(LocalDate date) {
        LocalDate previous = date.minusDays(1); // 先抓昨天
        while (previous.getDayOfWeek() == DayOfWeek.SATURDAY || previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
            previous = previous.minusDays(1);
        }
        return previous;
    }

}
