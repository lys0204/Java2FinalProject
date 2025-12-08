package org.example.stackflowanalysis.Controller;

import org.example.stackflowanalysis.Service.AnalysisService;
import org.example.stackflowanalysis.Service.DataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class DataController {

    private final DataService stackOverflowService;
    private final AnalysisService analysisService;

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Long> trendData = analysisService.getTopicTrend(tagName, start, end);
        return ResponseEntity.ok(trendData);
    }
    @GetMapping("/api/topNpairs")
    public ResponseEntity<List<Map.Entry<String, Integer>>> getTopCoOccurringTags(
            @RequestParam(defaultValue = "10") int topN) {
        List<Map.Entry<String, Integer>> topPairs = analysisService.getTopCoOccurringTags(topN);
        return ResponseEntity.ok(topPairs);
    }
    @GetMapping("/api/topNmonthly")
    public ResponseEntity<List<Map.Entry<String, Integer>>> getTopTagsMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(defaultValue = "10") int topN) {
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