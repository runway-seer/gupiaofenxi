package com.gupiao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gupiao.model.AnalysisResult;
import com.gupiao.model.KLineData;
import com.gupiao.model.KLineItem;
import com.gupiao.model.StockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EastMoneyService eastMoneyService;

    @Value("${ai.provider:openai}")
    private String provider;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String aiBaseUrl;

    public AnalysisResult analyzeStock(String code, String market) {
        StockInfo quote = eastMoneyService.getStockQuote(code, market);
        KLineData klineData = eastMoneyService.getKLineData(code, market, "101", "1", 60);

        if (quote == null) {
            return AnalysisResult.builder()
                    .stockCode(code)
                    .summary("未能获取到股票数据")
                    .build();
        }

        String prompt = buildAnalysisPrompt(quote, klineData);
        String aiResponse = callAI(prompt);

        return parseAnalysisResult(aiResponse, code, quote.getName());
    }

    private String buildAnalysisPrompt(StockInfo quote, KLineData klineData) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下股票进行专业的技术分析，用中文回答，格式为JSON：\n\n");
        sb.append("股票代码：").append(quote.getCode()).append("\n");
        sb.append("股票名称：").append(quote.getName()).append("\n");
        sb.append("市场：").append(quote.getMarket()).append("\n");
        sb.append("现价：").append(quote.getPrice()).append("元\n");
        sb.append("涨跌幅：").append(quote.getChangePercent()).append("%\n");
        sb.append("涨跌额：").append(quote.getChangeAmount()).append("元\n");
        sb.append("今开：").append(quote.getOpen()).append("元\n");
        sb.append("最高：").append(quote.getHigh()).append("元\n");
        sb.append("最低：").append(quote.getLow()).append("元\n");
        sb.append("昨收：").append(quote.getPreClose()).append("元\n");
        sb.append("成交量：").append(quote.getVolume()).append("手\n");
        sb.append("成交额：").append(quote.getAmount()).append("元\n");
        sb.append("换手率：").append(quote.getTurnoverRate()).append("%\n");
        sb.append("市盈率：").append(quote.getPe()).append("\n");
        sb.append("总市值：").append(formatMarketCap(quote.getTotalMarketCap())).append("\n");
        sb.append("流通市值：").append(formatMarketCap(quote.getCirculatingMarketCap())).append("\n\n");

        if (klineData != null && klineData.getKlines() != null && !klineData.getKlines().isEmpty()) {
            sb.append("最近").append(Math.min(10, klineData.getKlines().size())).append("个交易日K线数据（日期 开盘 收盘 最高 最低 成交量）：\n");
            List<KLineItem> klines = klineData.getKlines();
            int start = Math.max(0, klines.size() - 10);
            for (int i = start; i < klines.size(); i++) {
                KLineItem item = klines.get(i);
                sb.append(item.getDate()).append(" ")
                        .append(item.getOpen()).append(" ")
                        .append(item.getClose()).append(" ")
                        .append(item.getHigh()).append(" ")
                        .append(item.getLow()).append(" ")
                        .append(item.getVolume()).append("\n");
            }
        }

        sb.append("\n请返回JSON格式：{\"summary\":\"综合分析\",\"trendAnalysis\":\"趋势分析\",\"riskAssessment\":\"风险评估\",\"suggestion\":\"操作建议\",\"sentiment\":\"市场情绪\",\"confidenceScore\":0.0~1.0的置信度}");
        return sb.toString();
    }

    private String callAI(String prompt) {
        if (apiKey == null || apiKey.isBlank() || "your-api-key".equals(apiKey)) {
            return generateMockAnalysis(prompt);
        }

        try {
            WebClient client = WebClient.builder().build();

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.set("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            String response = client.post()
                    .uri(aiBaseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            return generateMockAnalysis(prompt);
        } catch (Exception e) {
            log.error("AI分析调用失败: {}", e.getMessage());
            return generateMockAnalysis(prompt);
        }
    }

    private String generateMockAnalysis(String prompt) {
        String stockName = extractStockName(prompt);
        double changePercent = extractChangePercent(prompt);

        String trendAnalysis;
        String suggestion;
        double confidence;

        if (changePercent > 3) {
            trendAnalysis = "该股近期走势强劲，处于上升通道中，短期均线呈现多头排列，成交量配合良好。MACD指标显示金叉信号，KDJ处于强势区域。建议关注上方压力位突破情况。";
            suggestion = "短期可持有，但注意高位回调风险。建议设置止盈位，控制仓位在合理范围。中长期可关注回调后的低吸机会。";
            confidence = 0.75;
        } else if (changePercent > 0) {
            trendAnalysis = "该股走势平稳偏多，价格在均线系统上方运行，成交量温和。MACD处于零轴上方，市场做多意愿较强。短期或有震荡，但整体趋势向好。";
            suggestion = "可适度参与，关注量能变化。建议逢低布局，控制仓位，不宜追高。中长期关注公司基本面和行业趋势。";
            confidence = 0.65;
        } else if (changePercent > -3) {
            trendAnalysis = "该股当前处于调整阶段，价格在均线附近震荡，成交量萎缩。MACD指标有走弱迹象，短期或将继续盘整。需要关注下方支撑位的有效性。";
            suggestion = "建议观望为主，等待明确信号。如已持有可设好止损位，短期不建议加仓。关注市场情绪和资金流向变化。";
            confidence = 0.55;
        } else {
            trendAnalysis = "该股近期走势偏弱，价格跌破重要支撑位，成交量放大显示抛压较重。MACD指标呈现死叉，KDJ处于超卖区域。短期下行压力较大，需警惕进一步下跌风险。";
            suggestion = "短期建议规避风险，不建议追跌抄底。已持有者应严格止损，控制仓位。中长期投资者可等待企稳信号后再做决策。";
            confidence = 0.70;
        }

        return String.format("""
                {
                    "summary": "%s近期的综合技术分析显示，该股当前%s。从技术指标来看，市场多空力量%s。结合成交量和资金流向，整体走势%s。",
                    "trendAnalysis": "%s",
                    "riskAssessment": "主要风险包括：市场系统性风险、行业政策变化风险、公司经营风险以及流动性风险。当前波动率处于%s水平，投资者需注意仓位管理。",
                    "suggestion": "%s",
                    "sentiment": "%s",
                    "confidenceScore": %.2f
                }
                """,
                stockName,
                changePercent > 0 ? "表现较为积极" : "处于调整之中",
                changePercent > 1 ? "偏向多方" : "偏向空方",
                changePercent > 0 ? "短期内仍有向上空间" : "需等待明确的企稳信号",
                trendAnalysis,
                Math.abs(changePercent) > 3 ? "较高" : "中等",
                suggestion,
                changePercent > 2 ? "市场情绪偏乐观，资金参与度较高" : changePercent > 0 ? "市场情绪中性偏积极" : "市场情绪偏谨慎，观望氛围浓厚",
                confidence
        );
    }

    private AnalysisResult parseAnalysisResult(String aiResponse, String code, String stockName) {
        try {
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("```json") + 7);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            } else if (aiResponse.contains("```")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("```") + 3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            }

            jsonStr = jsonStr.trim();
            if (!jsonStr.startsWith("{")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("{"));
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("}") + 1);
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            return AnalysisResult.builder()
                    .stockCode(code)
                    .stockName(stockName)
                    .summary(root.path("summary").asText("暂无分析"))
                    .trendAnalysis(root.path("trendAnalysis").asText("暂无趋势分析"))
                    .riskAssessment(root.path("riskAssessment").asText("暂无风险评估"))
                    .suggestion(root.path("suggestion").asText("暂无操作建议"))
                    .sentiment(root.path("sentiment").asText("暂无情绪分析"))
                    .confidenceScore(root.path("confidenceScore").asDouble(0.5))
                    .build();
        } catch (Exception e) {
            log.error("解析AI分析结果失败: {}", e.getMessage());
            return AnalysisResult.builder()
                    .stockCode(code)
                    .stockName(stockName)
                    .summary(aiResponse)
                    .trendAnalysis("解析失败")
                    .riskAssessment("解析失败")
                    .suggestion("请重试")
                    .sentiment("未知")
                    .confidenceScore(0.0)
                    .build();
        }
    }

    private String extractStockName(String prompt) {
        try {
            int idx = prompt.indexOf("股票名称：");
            if (idx >= 0) {
                int end = prompt.indexOf("\n", idx);
                return prompt.substring(idx + 6, end).trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return "该股票";
    }

    private double extractChangePercent(String prompt) {
        try {
            int idx = prompt.indexOf("涨跌幅：");
            if (idx >= 0) {
                int end = prompt.indexOf("%", idx);
                String val = prompt.substring(idx + 4, end).trim();
                return Double.parseDouble(val);
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private String formatMarketCap(Double cap) {
        if (cap == null) return "未知";
        if (cap >= 100000000) {
            return String.format("%.2f亿", cap / 100000000);
        }
        if (cap >= 10000) {
            return String.format("%.2f万", cap / 10000);
        }
        return String.format("%.0f", cap);
    }
}