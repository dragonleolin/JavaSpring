package com.example.autoDemo.data;

import java.io.Serializable;

public class StockResponse implements Serializable {

    private String code;
    private String name;
    private String price;
    private String marketTime;
    private Double k;
    private Double d;

    // Jackson 反序列化需要預設建構子
    public StockResponse() {}

    public StockResponse(String code, String name, String price, String marketTime, Double k, Double d) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.marketTime = marketTime;
        this.k = k;
        this.d = d;
    }

    // getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getMarketTime() { return marketTime; }
    public void setMarketTime(String marketTime) { this.marketTime = marketTime; }

    public Double getK() {
        return k;
    }

    public void setK(Double k) {
        this.k = k;
    }

    public Double getD() {
        return d;
    }

    public void setD(Double d) {
        this.d = d;
    }
}
