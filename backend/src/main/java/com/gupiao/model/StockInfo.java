package com.gupiao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {
    private String code;
    private String name;
    private String market;
    private Double price;
    private Double changePercent;
    private Double changeAmount;
    private Double volume;
    private Double amount;
    private Double high;
    private Double low;
    private Double open;
    private Double preClose;
    private Double turnoverRate;
    private Double pe;
    private Double totalMarketCap;
    private Double circulatingMarketCap;
}