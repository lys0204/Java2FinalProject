package org.example.stackflowanalysis.Service;

import org.example.stackflowanalysis.Data.*;
import org.example.stackflowanalysis.Repositories.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 只读事务，提高性能
public class AnalysisService {

    private final QuestionRepository questionRepository;

    public AnalysisService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // ==========================================
    // Task 1: Topic Trends (趋势分析)
    // [cite: 47, 49] - 分析特定 Topic 在一段时间内的活跃度
    // ==========================================
    public Map<String, Long> getTopicTrend(String tagName, LocalDateTime start, LocalDateTime end) {
        List<Question> questions = questionRepository.findByTagAndDateRange(tagName, start, end);

        // 使用 Stream 按月分组统计数量
        return questions.stream()
                .map(q -> q.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));
    }

    // ==========================================
    // Task 2: Co-occurrence of Topics (共现分析)
    // [cite: 52, 54] - 找出经常一起出现的 Tag 对
    // ==========================================
    public List<Map.Entry<String, Integer>> getTopCoOccurringTags(int topN) {
        List<Question> allQuestions = questionRepository.findAllWithTags();
        Map<String, Integer> pairCounts = new HashMap<>();

        for (Question q : allQuestions) {
            List<String> tags = q.getTags().stream()
                    .map(Tag::getName)
                    .sorted() // 排序确保 (java, spring) 和 (spring, java) 是同一个 Key
                    .toList();

            // 生成两两组合
            for (int i = 0; i < tags.size(); i++) {
                for (int j = i + 1; j < tags.size(); j++) {
                    String pair = tags.get(i) + " + " + tags.get(j);
                    pairCounts.put(pair, pairCounts.getOrDefault(pair, 0) + 1);
                }
            }
        }

        // 排序并取 Top N
        return pairCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 降序
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ==========================================
    // Task 3: Common Pitfalls in Multithreading
    // [cite: 56, 61] - 内容分析 (NLP/Regex)
    // ==========================================
    public List<String> analyzeMultithreadingPitfalls() {
        // 定义相关的 Tag 集合
        List<String> targetTags = Arrays.asList("multithreading", "concurrency", "thread-safety", "java");

        List<Question> threads = questionRepository.findByTagNamesWithContent(targetTags);

        // 这里需要你实现正则匹配或关键词统计逻辑
        // 示例：统计异常类型 (NullPointerException, Deadlock, etc.)
        Map<String, Long> keywordCounts = new HashMap<>();

        for (Question q : threads) {
            String fullText = q.getTitle() + " " + q.getContent();
            // 简单的关键词匹配示例
            if (fullText.toLowerCase().contains("deadlock")) {
                keywordCounts.merge("Deadlock", 1L, Long::sum);
            }
            if (fullText.toLowerCase().contains("race condition")) {
                keywordCounts.merge("Race Condition", 1L, Long::sum);
            }
            // ... 更多正则匹配
        }

        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.toList());
    }

    // ==========================================
    // Task 4: Solvable vs. Hard-to-Solve
    // [cite: 66, 67] - 对比两组问题的特征
    // ==========================================
    public Map<String, Object> compareSolvability() {
        List<Question> all = questionRepository.findAllWithAnswers();

        // 定义 Solvable: 有被采纳的回答 OR 有得分为正的回答
        List<Question> solvable = all.stream()
                .filter(q -> q.getAnswers().stream()
                        .anyMatch(a -> a.isAccepted() || a.getScore() > 0))
                .toList();

        // 定义 Hard: 无采纳回答 AND (无回答 OR 回答得分均 <= 0)
        List<Question> hard = all.stream()
                .filter(q -> !solvable.contains(q))
                .toList();

        // 分析特征：例如平均长度、平均浏览量等
        double avgViewSolvable = solvable.stream().mapToInt(Question::getViewCount).average().orElse(0);
        double avgViewHard = hard.stream().mapToInt(Question::getViewCount).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("Solvable Count", solvable.size());
        result.put("Hard Count", hard.size());
        result.put("Avg View (Solvable)", avgViewSolvable);
        result.put("Avg View (Hard)", avgViewHard);

        return result;
    }
}