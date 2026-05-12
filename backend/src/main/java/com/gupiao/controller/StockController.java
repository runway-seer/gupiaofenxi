package com.gupiao.controller;

import com.gupiao.dto.ApiResponse;
import com.gupiao.model.KLineData;
import com.gupiao.model.StockInfo;
import com.gupiao.service.EastMoneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final EastMoneyService eastMoneyService;

    @GetMapping("/list")
    public ApiResponse<List<StockInfo>> getStockList(
            @RequestParam(defaultValue = "all") String market,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<StockInfo> stocks = eastMoneyService.getStockList(market, page, pageSize);
        return ApiResponse.success(stocks);
    }

    @GetMapping("/quote")
    public ApiResponse<StockInfo> getQuote(
            @RequestParam String code,
            @RequestParam(defaultValue = "") String market) {
        StockInfo stock = eastMoneyService.getStockQuote(code, market);
        if (stock == null) {
            return ApiResponse.error(404, "未找到该股票");
        }
        return ApiResponse.success(stock);
    }

    @GetMapping("/kline")
    public ApiResponse<KLineData> getKLine(
            @RequestParam String code,
            @RequestParam(defaultValue = "") String market,
            @RequestParam(defaultValue = "101") String klt,
            @RequestParam(defaultValue = "1") String fqt,
            @RequestParam(defaultValue = "60") int count) {
        KLineData data = eastMoneyService.getKLineData(code, market, klt, fqt, count);
        return ApiResponse.success(data);
    }

    @GetMapping("/search")
    public ApiResponse<List<StockInfo>> searchStocks(@RequestParam String keyword) {
        List<StockInfo> stocks = eastMoneyService.searchStocks(keyword);
        return ApiResponse.success(stocks);
    }
}