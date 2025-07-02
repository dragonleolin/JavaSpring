package com.example.autoDemo.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockInfo {
    private String code;
    private String name;
    private String price;
    private String marketTime;
}


