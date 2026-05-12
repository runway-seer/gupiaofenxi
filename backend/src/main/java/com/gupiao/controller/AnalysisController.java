package com.gupiao.controller;

import com.gupiao.dto.ApiResponse;
import com.gupiao.model.AnalysisResult;
import com.gupiao.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/stock")
    public ApiResponse<AnalysisResult> analyzeStock(
            @RequestParam String code,
            @RequestParam(defaultValue = "") String market) {
        AnalysisResult result = analysisService.analyzeStock(code, market);
        return ApiResponse.success(result);
    }
}