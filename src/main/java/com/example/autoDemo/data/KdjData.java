package com.example.autoDemo.data;

public class KdjData {
    private String date;
    private Double k;
    private Double d;
    private Double j;

    public KdjData(String date, Double k, Double d, Double j) {
        this.date = date;
        this.k = k;
        this.d = d;
        this.j = j;
    }
    @Override
    public String toString() {
        return "KdjData{" +
                "date='" + date + '\'' +
                ", k=" + k +
                ", d=" + d +
                ", j=" + j +
                '}';
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

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

    public Double getJ() {
        return j;
    }

    public void setJ(Double j) {
        this.j = j;
    }
}
