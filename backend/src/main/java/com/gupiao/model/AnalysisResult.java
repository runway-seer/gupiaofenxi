package com.gupiao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private String stockCode;
    private String stockName;
    private String summary;
    private String trendAnalysis;
    private String riskAssessment;
    private String suggestion;
    private String sentiment;
    private Double confidenceScore;
}