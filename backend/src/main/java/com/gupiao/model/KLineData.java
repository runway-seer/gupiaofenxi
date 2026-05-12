package com.gupiao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KLineData {
    private String code;
    private String name;
    private String market;
    private List<KLineItem> klines;
}