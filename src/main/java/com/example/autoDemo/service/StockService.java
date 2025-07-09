package com.example.autoDemo.service;

import com.example.autoDemo.data.StockResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    @Autowired
    RedisService redisService;
    @Autowired
    private RedisTemplate<String, StockResponse> redisTemplate;

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

}
