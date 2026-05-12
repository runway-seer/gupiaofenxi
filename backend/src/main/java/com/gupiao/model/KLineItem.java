package com.gupiao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KLineItem {
    private String date;
    private Double open;
    private Double close;
    private Double high;
    private Double low;
    private Double volume;
    private Double amount;
    private Double changePercent;
    private Double turnoverRate;
}