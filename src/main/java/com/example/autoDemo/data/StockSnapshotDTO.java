package com.example.autoDemo.data;
import lombok.Data;

@Data
public class StockSnapshotDTO {
    private String symbol;       // 股票代號
    private String name;         // 股票名稱
    private Double openPrice;    // 開盤
    private Double highPrice;    // 最高
    private Double lowPrice;     // 最低
    private Double closePrice;   // 收盤
    private Double change;       // 漲跌
    private Double changePercent;// 漲跌幅 %
    private Long tradeVolume;    // 成交量
    private Long tradeValue;     // 成交金額
}
