package com.example.autoDemo.data;

import java.util.List;
public class FugleKdjResponse {
    private String symbol;
    private String from;
    private String to;
    private String timeframe;
    private int rPeriod;
    private int kPeriod;
    private int dPeriod;
    private List<KdjData> data;

    public FugleKdjResponse(String symbol, String from, String to, String timeframe, int rPeriod, int kPeriod, int dPeriod, List<KdjData> data) {
        this.symbol = symbol;
        this.from = from;
        this.to = to;
        this.timeframe = timeframe;
        this.rPeriod = rPeriod;
        this.kPeriod = kPeriod;
        this.dPeriod = dPeriod;
        this.data = data;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public int getrPeriod() {
        return rPeriod;
    }

    public void setrPeriod(int rPeriod) {
        this.rPeriod = rPeriod;
    }

    public int getkPeriod() {
        return kPeriod;
    }

    public void setkPeriod(int kPeriod) {
        this.kPeriod = kPeriod;
    }

    public int getdPeriod() {
        return dPeriod;
    }

    public void setdPeriod(int dPeriod) {
        this.dPeriod = dPeriod;
    }

    public List<KdjData> getData() {
        return data;
    }

    public void setData(List<KdjData> data) {
        this.data = data;
    }
}
