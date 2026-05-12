package com.gupiao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gupiao.model.KLineData;
import com.gupiao.model.KLineItem;
import com.gupiao.model.StockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EastMoneyService {

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${eastmoney.api.base-url}")
    private String baseUrl;

    @Value("${eastmoney.api.kline-url}")
    private String klineUrl;

    public List<StockInfo> getStockList(String market, int page, int pageSize) {
        String fs = buildMarketFilter(market);
        String fields = "f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f14,f15,f16,f17,f18,f20,f21,f100";

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/qt/clist/get")
                .queryParam("pn", page)
                .queryParam("pz", pageSize)
                .queryParam("po", "1")
                .queryParam("np", "1")
                .queryParam("fltt", "2")
                .queryParam("invt", "2")
                .queryParam("fid", "f3")
                .queryParam("fs", fs)
                .queryParam("fields", fields)
                .toUriString();

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://quote.eastmoney.com/")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode diff = data.path("diff");
            if (diff == null || !diff.isArray()) {
                return List.of();
            }

            List<StockInfo> stocks = new ArrayList<>();
            for (JsonNode node : diff) {
                StockInfo stock = StockInfo.builder()
                        .code(node.path("f12").asText())
                        .name(node.path("f14").asText())
                        .market(node.path("f13").asInt() == 1 ? "SH" : "SZ")
                        .price(node.path("f2").isNull() ? null : node.path("f2").asDouble())
                        .changePercent(node.path("f3").isNull() ? null : node.path("f3").asDouble())
                        .changeAmount(node.path("f4").isNull() ? null : node.path("f4").asDouble())
                        .volume(node.path("f5").isNull() ? null : node.path("f5").asDouble())
                        .amount(node.path("f6").isNull() ? null : node.path("f6").asDouble())
                        .high(node.path("f15").isNull() ? null : node.path("f15").asDouble())
                        .low(node.path("f16").isNull() ? null : node.path("f16").asDouble())
                        .open(node.path("f17").isNull() ? null : node.path("f17").asDouble())
                        .preClose(node.path("f18").isNull() ? null : node.path("f18").asDouble())
                        .turnoverRate(node.path("f8").isNull() ? null : node.path("f8").asDouble())
                        .pe(node.path("f9").isNull() ? null : node.path("f9").asDouble())
                        .totalMarketCap(node.path("f20").isNull() ? null : node.path("f20").asDouble())
                        .circulatingMarketCap(node.path("f21").isNull() ? null : node.path("f21").asDouble())
                        .build();
                stocks.add(stock);
            }
            return stocks;
        } catch (Exception e) {
            log.error("获取股票列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    public KLineData getKLineData(String code, String market, String klt, String fqt, int count) {
        String secid = buildSecid(code, market);
        String fields1 = "f1,f2,f3,f4,f5,f6";
        String fields2 = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

        String url = UriComponentsBuilder.fromHttpUrl(klineUrl + "/api/qt/stock/kline/get")
                .queryParam("secid", secid)
                .queryParam("ut", "fa5fd1943c7b386f172d6893dbfba10b")
                .queryParam("fields1", fields1)
                .queryParam("fields2", fields2)
                .queryParam("klt", klt)
                .queryParam("fqt", fqt)
                .queryParam("end", "20500101")
                .queryParam("lmt", count)
                .toUriString();

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://quote.eastmoney.com/")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode klines = data.path("klines");

            if (klines == null || !klines.isArray()) {
                return KLineData.builder().code(code).build();
            }

            List<KLineItem> items = new ArrayList<>();
            for (JsonNode line : klines) {
                String[] parts = line.asText().split(",");
                if (parts.length >= 8) {
                    KLineItem item = KLineItem.builder()
                            .date(parts[0])
                            .open(parseDouble(parts[1]))
                            .close(parseDouble(parts[2]))
                            .high(parseDouble(parts[3]))
                            .low(parseDouble(parts[4]))
                            .volume(parseDouble(parts[5]))
                            .amount(parseDouble(parts[6]))
                            .changePercent(parts.length > 8 ? parseDouble(parts[8]) : null)
                            .turnoverRate(parts.length > 10 ? parseDouble(parts[10]) : null)
                            .build();
                    items.add(item);
                }
            }

            return KLineData.builder()
                    .code(code)
                    .name(data.path("name").asText())
                    .market(market)
                    .klines(items)
                    .build();
        } catch (Exception e) {
            log.error("获取K线数据失败: {}", e.getMessage());
            return KLineData.builder().code(code).build();
        }
    }

    public StockInfo getStockQuote(String code, String market) {
        String secid = buildSecid(code, market);
        String fields = "f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f116,f117,f162,f167,f168,f169,f170,f171";

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/qt/stock/get")
                .queryParam("secid", secid)
                .queryParam("ut", "fa5fd1943c7b386f172d6893dbfba10b")
                .queryParam("fields", fields)
                .queryParam("invt", "2")
                .queryParam("fltt", "2")
                .toUriString();

        try {
            String response = webClient.get()
                    .uri(url)
                    .header("Referer", "https://quote.eastmoney.com/")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            return StockInfo.builder()
                    .code(code)
                    .name(data.path("f58").asText())
                    .market(market)
                    .price(data.path("f43").isNull() ? null : data.path("f43").asDouble() / 100.0)
                    .changePercent(data.path("f170").isNull() ? null : data.path("f170").asDouble() / 100.0)
                    .changeAmount(data.path("f169").isNull() ? null : data.path("f169").asDouble() / 100.0)
                    .high(data.path("f44").isNull() ? null : data.path("f44").asDouble() / 100.0)
                    .low(data.path("f45").isNull() ? null : data.path("f45").asDouble() / 100.0)
                    .open(data.path("f46").isNull() ? null : data.path("f46").asDouble() / 100.0)
                    .preClose(data.path("f60").isNull() ? null : data.path("f60").asDouble() / 100.0)
                    .volume(data.path("f47").isNull() ? null : data.path("f47").asDouble())
                    .amount(data.path("f48").isNull() ? null : data.path("f48").asDouble())
                    .turnoverRate(data.path("f168").isNull() ? null : data.path("f168").asDouble() / 100.0)
                    .pe(data.path("f162").isNull() ? null : data.path("f162").asDouble() / 100.0)
                    .totalMarketCap(data.path("f116").isNull() ? null : data.path("f116").asDouble())
                    .circulatingMarketCap(data.path("f117").isNull() ? null : data.path("f117").asDouble())
                    .build();
        } catch (Exception e) {
            log.error("获取股票行情失败: {}", e.getMessage());
            return null;
        }
    }

    public List<StockInfo> searchStocks(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String url = "https://searchadapter.eastmoney.com/api/suggest/get";

        try {
            String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("input", keyword)
                    .queryParam("type", "14")
                    .queryParam("token", "D43BF722C8E33BDC906FB84D85E326E8")
                    .queryParam("count", "20")
                    .toUriString();

            String response = webClient.get()
                    .uri(fullUrl)
                    .header("Referer", "https://www.eastmoney.com/")
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode quotes = root.path("QuotationCodeTable").path("Data");
            if (quotes == null || !quotes.isArray()) {
                return List.of();
            }

            List<StockInfo> stocks = new ArrayList<>();
            for (JsonNode node : quotes) {
                String code = node.path("Code").asText();
                String market = node.path("MktNum").asText();
                if (code.isEmpty()) {
                    continue;
                }
                stocks.add(StockInfo.builder()
                        .code(code)
                        .name(node.path("Name").asText())
                        .market(market)
                        .build());
            }
            return stocks;
        } catch (Exception e) {
            log.error("搜索股票失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildMarketFilter(String market) {
        if ("sh".equalsIgnoreCase(market)) {
            return "m:1+t:2,m:1+t:23";
        } else if ("sz".equalsIgnoreCase(market)) {
            return "m:0+t:6,m:0+t:80";
        } else if ("bj".equalsIgnoreCase(market)) {
            return "m:0+t:81";
        } else if ("cyb".equalsIgnoreCase(market)) {
            return "m:0+t:80";
        } else if ("kcb".equalsIgnoreCase(market)) {
            return "m:1+t:23";
        }
        return "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23";
    }

    private String buildSecid(String code, String market) {
        if ("SH".equalsIgnoreCase(market) || "1".equals(market)) {
            return "1." + code;
        } else if ("SZ".equalsIgnoreCase(market) || "0".equals(market)) {
            return "0." + code;
        } else if ("BJ".equalsIgnoreCase(market)) {
            return "0." + code;
        }
        return code.startsWith("6") ? "1." + code : "0." + code;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}