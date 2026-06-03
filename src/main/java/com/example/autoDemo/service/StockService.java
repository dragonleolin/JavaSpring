package com.example.autoDemo.service;

import com.example.autoDemo.data.FugleKdjResponse;
import com.example.autoDemo.data.KdjData;
import com.example.autoDemo.data.StockResponse;
import com.example.autoDemo.data.StockSnapshotDTO;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String BASE_URL = "https://api.fugle.tw/marketdata/v1.0/stock/snapshot";

    public List<StockResponse> getStockInfo(List<String> stockCodes) {
        List<StockResponse> responseList = new ArrayList<>();

        // 取得前一營業日的日期
        LocalDate previousDay = getPreviousWorkday(LocalDate.now());
        String previousDayStr = previousDay.format(formatter);

        for (String code : stockCodes) {
            // 先查 Redis 緩存 (這裡能防止頻繁 F5 刷新導致 Line 瘋狂連發通知)
            StockResponse cached = redisService.getFromCache(code);
            if (cached != null) {
                responseList.add(cached);
                continue;
            }

            try {
                // 1. 查詢 TWSE 股票基本資訊
                String url = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp?ex_ch=tse_" + code + ".tw";
                String response = restTemplate.getForObject(url, String.class);

                JSONObject json = new JSONObject(response);
                JSONObject stock = json.getJSONArray("msgArray").getJSONObject(0);
                LocalDateTime now = LocalDateTime.now();
                String formattedTime = now.format(DATE_TIME_FORMATTER);

                String name = stock.getString("n");
                String price = stock.getString("z");

                if ("-".equals(price)) {
                    price = stock.getString("h"); // 若無成交價，取最高價
                }

                // 2. 查詢最新的 KD 值
                KdjData kdjData = getLatestKdj(code, previousDayStr, previousDayStr);

                StockResponse stockResponse = new StockResponse();
                stockResponse.setCode(code);
                stockResponse.setName(name);
                stockResponse.setPrice(price);
                stockResponse.setMarketTime(formattedTime);

                // 3. 整合：寫入 KD 值並判斷是否需要發送通知
                if (kdjData != null) {
                    double kValue = Math.round(kdjData.getK() * 100.0) / 100.0;
                    double dValue = Math.round(kdjData.getD() * 100.0) / 100.0;
                    stockResponse.setK(kValue);
                    stockResponse.setD(dValue);

                    // 🚨 核心邏輯：判斷 KD 高低檔並觸發 Kafka 發送 Line 警示
                    try {
                        String alertType = "";

                        // 定義高低檔水位線 (可依策略微調，例如高檔設為 80)
                        if (kValue <= 25) {
                            alertType = "📉 [KD低檔] 超賣警示 / 關注反彈";
                        } else if (kValue >= 88) {
                            alertType = "📈 [KD高檔] 超買警示 / 注意回檔";
                        }

                        // 只有當 alertType 被賦值時 (觸發高低檔條件)，才發送通知
                        if (!alertType.isEmpty()) {
                            String alertMsg = String.format("%s\n股票：%s %s\nK=%.2f, D=%.2f\n時間：%s",
                                    alertType, code, name, kValue, dValue, kdjData.getDate());

                            kafkaProducerService.sendLineMessage(alertMsg);
                            System.out.println("✅ 已發送 KD 警示通知: " + code + " (" + alertType + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 發送 Line 通知失敗: " + e.getMessage());
                    }

                } else {
                    stockResponse.setK(0.0);
                    stockResponse.setD(0.0);
                }

                // 寫入 Redis 快取 (10 分鐘) - 這同時發揮了「10分鐘內不重複發送相同警告」的保護作用
                redisService.saveToCache(code, stockResponse);

                // 寫入 Redis 歷史資料
                redisService.saveToHistory(code, formattedTime, stockResponse);

                responseList.add(stockResponse);
            } catch (Exception e) {
                System.err.println("❌ 股票 " + code + " 查詢失敗: " + e.getMessage());
                responseList.add(new StockResponse(code, "查詢失敗", "-", "-", 0.0, 0.0));
            }
        }

        return responseList;
    }

    public KdjData getLatestKdj(String symbol, String fromDate, String toDate) {
        int maxRetries = 5;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String url = String.format(
                        "https://api.fugle.tw/marketdata/v1.0/stock/technical/kdj/%s?from=%s&to=%s&timeframe=D&rPeriod=9&kPeriod=3&dPeriod=3",
                        symbol, fromDate, toDate);

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-KEY", fugleToken);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<FugleKdjResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, FugleKdjResponse.class);

                if (response.getStatusCode() == HttpStatus.OK &&
                        response.getBody() != null &&
                        !response.getBody().getData().isEmpty()) {

                    return response.getBody().getData()
                            .get(response.getBody().getData().size() - 1); // 取最後一筆
                }

            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("⚠️ 查無資料，嘗試往前一天重試...（第 " + (retryCount + 1) + " 次）");

                // 🛠️ 修正點：將當前的 fromDate 轉換回 LocalDate，再往前推一天
                LocalDate currentQueryDate = LocalDate.parse(fromDate, formatter);
                LocalDate previousWorkday = getPreviousWorkday(currentQueryDate);

                // 更新 fromDate 與 toDate，讓下一次迴圈查更早的一天
                fromDate = previousWorkday.format(formatter);
                toDate = previousWorkday.format(formatter);
                retryCount++;

            } catch (Exception e) {
                System.err.println("🔥 API 其他錯誤: " + e.getMessage());
                break;
            }
        }

        System.out.println("❌ [" + symbol + "] 超過最大重試次數 (" + maxRetries + ") 仍無法取得 KD 資料");
        return null;
    }

    // 成交量排行
    public List<StockSnapshotDTO> getActiveStocks(String market) {
        String url = "https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL";
        String response = restTemplate.getForObject(url, String.class);
        System.out.println("✅ response: " + response);
        return fetchStockList(url);
    }

    // 漲跌幅排行
    public List<StockSnapshotDTO> getMoverStocks(String market) {
        String url = BASE_URL + "/movers/" + market + "?direction=up&change=percent";
        return fetchStockList(url);
    }

    @SuppressWarnings("unchecked")
    private List<StockSnapshotDTO> fetchStockList(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", fugleToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

        List<StockSnapshotDTO> result = new ArrayList<>();
        for (Map<String, Object> item : data) {
            StockSnapshotDTO dto = new StockSnapshotDTO();
            dto.setSymbol((String) item.get("symbol"));
            dto.setName((String) item.get("name"));
            dto.setOpenPrice(toDouble(item.get("openPrice")));
            dto.setHighPrice(toDouble(item.get("highPrice")));
            dto.setLowPrice(toDouble(item.get("lowPrice")));
            dto.setClosePrice(toDouble(item.get("closePrice")));
            dto.setChange(toDouble(item.get("change")));
            dto.setChangePercent(toDouble(item.get("changePercent")));
            dto.setTradeVolume(toLong(item.get("tradeVolume")));
            dto.setTradeValue(toLong(item.get("tradeValue")));
            result.add(dto);
        }
        return result;
    }

    // 小工具：避免型別轉換出錯
    private Double toDouble(Object o) {
        return o == null ? null : Double.valueOf(o.toString());
    }

    private Long toLong(Object o) {
        return o == null ? null : Long.valueOf(o.toString());
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

    /**
     * 輔助方法：取得前一個工作日 (避開六日)
     */
    public static LocalDate getPreviousWorkday(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        while (previous.getDayOfWeek() == DayOfWeek.SATURDAY || previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
            previous = previous.minusDays(1);
        }
        return previous;
    }

}
