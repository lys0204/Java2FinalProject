package org.example.stackflowanalysis.Controller;

import org.example.stackflowanalysis.Service.AnalysisService;
import org.example.stackflowanalysis.Service.DataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class DataController {

    private final DataService stackOverflowService;
    private final AnalysisService analysisService;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public DataController(DataService stackOverflowService, AnalysisService analysisService) {
        this.stackOverflowService = stackOverflowService;
        this.analysisService = analysisService;
    }

    @GetMapping("/api/collect")
    public String triggerCollection() {
        new Thread(stackOverflowService::collectData).start();
        return "Data collection started in background. Check console logs.";
    }
    @GetMapping("/api/trend")
    public ResponseEntity<Map<String, Long>> getTagTrend(
            @RequestParam String tagName,
            @RequestParam String starttime,
            @RequestParam String endtime) {
        boolean startmatch = Pattern.matches("^\\d{4}-(0[1-9]|1[0-2])$", starttime);
        boolean endmatch = Pattern.matches("^\\d{4}-(0[1-9]|1[0-2])$", endtime);
        if (startmatch && endmatch) {
            YearMonth startYearMonth = YearMonth.parse(starttime);
            YearMonth endYearMonth = YearMonth.parse(endtime);
            LocalDateTime start = startYearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = endYearMonth.atDay(1).atStartOfDay();
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("结束时间必须位于开始时间之后");
            }
            Map<String, Long> trendData = analysisService.getTopicTrend(tagName, start, end);
            return ResponseEntity.ok(trendData);
        }
        throw new IllegalArgumentException("时间格式错误");
    }
    @GetMapping("/api/topNpairs")
    public ResponseEntity<List<Map.Entry<String, Integer>>> getTopCoOccurringTags(
            @RequestParam(defaultValue = "10") String topNStr) {
        int topN;
        try {
            topN = Integer.parseInt(topNStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("topN 必须是有效的整数");
        }
        if (topN < 1 || topN > 100) {
            throw new IllegalArgumentException("topN 必须在 1 到 100 之间");
        }
        List<Map.Entry<String, Integer>> topPairs = analysisService.getTopCoOccurringTags(topN);
        return ResponseEntity.ok(topPairs);
    }
    @GetMapping("/api/topNmonthly")
    public ResponseEntity<List<Map.Entry<String, Integer>>> getTopTagsMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(defaultValue = "10") String topNStr) {
        int topN;
        try {
            topN = Integer.parseInt(topNStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("topN 必须是有效的整数");
        }
        if (topN < 1 || topN > 100) {
            throw new IllegalArgumentException("topN 必须在 1 到 100 之间");
        }
        List<Map.Entry<String, Integer>> topTags = analysisService.getTopNTagsmonthly(dateTime, topN);
        return ResponseEntity.ok(topTags);
    }
    @GetMapping("/api/wordcloud")
    public ResponseEntity<Map<String, Long>> getMultithreadingWordCloud() {

        Map<String, Long> wordCloudData = analysisService.generateMultithreadingWordCloud();
        return ResponseEntity.ok(wordCloudData);
    }
    @GetMapping("/api/solvability")
    public ResponseEntity<Map<String, Object>> getSolvabilityComparison() {

        Map<String, Object> comparisonData = analysisService.compareSolvability();
        return ResponseEntity.ok(comparisonData);
    }
}