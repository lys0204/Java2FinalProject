package org.example.stackflowanalysis.Service;

import org.example.stackflowanalysis.Data.*;
import org.example.stackflowanalysis.Repositories.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private final QuestionRepository questionRepository;

    public AnalysisService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // tag逐月统计数量象征热度
    public Map<String, Long> getTopicTrend(String tagName, LocalDateTime start, LocalDateTime end) {
        List<Question> questions = questionRepository.findByTagAndDateRange(tagName, start, end);
        return questions.stream()
                .map(q -> q.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));
    }
    // topN关联tag查询
    public List<Map.Entry<String, Integer>> getTopCoOccurringTags(int topN) {
        List<Question> allQuestions = questionRepository.findAllWithTags();
        Map<String, Integer> pairCounts = new HashMap<>();
        for (Question q : allQuestions) {
            List<String> tags = q.getTags().stream()
                    .map(Tag::getName)
                    .sorted()
                    .toList();
            for (int i = 0; i < tags.size(); i++) {
                for (int j = i + 1; j < tags.size(); j++) {
                    String pair = tags.get(i) + " + " + tags.get(j);
                    pairCounts.put(pair, pairCounts.getOrDefault(pair, 0) + 1);
                }
            }
        }
        return pairCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(topN)
                .collect(Collectors.toList());
    }
    // 某月topN的tag查询
    public List<Map.Entry<String, Integer>> getTopNTagsmonthly(LocalDateTime dateTime, int topN) {
        LocalDateTime start = dateTime.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime end = dateTime.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
        List<Question> questions = questionRepository.findWithTagsByMonth(start, end);
        Map<String, Integer> Counts = new HashMap<>();
        for (Question q : questions) {
            List<String> tags = q.getTags().stream()
                    .map(Tag::getName)
                    .sorted()
                    .toList();
            for (String tag : tags) {
                Counts.put(tag, Counts.getOrDefault(tag, 0) + 1);
            }
        }
        return Counts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(topN)
                .collect(Collectors.toList());
    }
    // 多线程常见问题统计
    public Map<String, Long> generateMultithreadingWordCloud() {
        List<String> targetTags = Arrays.asList("multithreading");
        List<Question> threads = questionRepository.findByTagNamesWithContent(targetTags);
        Set<String> stopWords = getStopWords();
        return threads.stream()
                .map(q -> (q.getTitle() + " " + q.getContent()).toLowerCase())
                .map(this::protectTechnicalPhrases)
                .map(this::cleanText)
                .flatMap(text -> Arrays.stream(text.split("\\s+")))
                .filter(word -> word.length() > 2 && !stopWords.contains(word))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // 降序
                .limit(50)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    // 分析问题解决的因素
    public Map<String, Object> compareSolvability() {
        List<Question> all = questionRepository.findAllWithAnswers();
        List<Question> solvable = all.stream()
                .filter(q -> q.getAnswers() != null && q.getAnswers().stream()
                        .anyMatch(a -> a.isAccepted() || a.getScore() > 0))
                .toList();
        List<Question> hard = all.stream()
                .filter(q -> !solvable.contains(q))
                .toList();
        Map<String, Object> result = new HashMap<>();
        long trendSolvable = countTagsinTime(solvable);
        long trendHard = countTagsinTime(hard);
        result.put("Trendiness", formatResult(trendSolvable, trendHard));
        double complexitySolvable = calculateAvgTagCount(solvable);
        double complexityHard = calculateAvgTagCount(hard);
        result.put("Complexity", formatResult(complexitySolvable, complexityHard));
        double detailSolvable = calculateAvgWordCount(solvable);
        double detailHard = calculateAvgWordCount(hard);
        result.put("Detail", formatResult(detailSolvable, detailHard));
        double repSolvable = calculateAvgAnswererReputation(solvable);
        double repHard = calculateAvgAnswererReputation(hard);
        result.put("Reputation", formatResult(repSolvable, repHard));
        double scoreSolvable = calculateAvgAnswerScore(solvable);
        double scoreHard = calculateAvgAnswerScore(hard);
        result.put("Answer Score", formatResult(scoreSolvable, scoreHard));
        return result;
    }
    // 辅助方法
    private String protectTechnicalPhrases(String text) {
        Map<String, String> phraseMap = new HashMap<>();
        phraseMap.put("race condition", "racecondition");
        phraseMap.put("dead lock", "deadlock"); // 归一化
        phraseMap.put("memory leak", "memory_leak");
        phraseMap.put("thread safe", "thread_safe");
        phraseMap.put("thread pool", "thread_pool");
        phraseMap.put("context switch", "context_switch");
        phraseMap.put("concurrent modification", "concurrent_modification");
        phraseMap.put("wait notify", "wait_notify");
        phraseMap.put("count down latch", "count_down_latch");
        phraseMap.put("atomic integer", "atomic_integer");
        String processed = text;
        for (Map.Entry<String, String> entry : phraseMap.entrySet()) {
            if (processed.contains(entry.getKey())) {
                processed = processed.replace(entry.getKey(), entry.getValue());
            }
        }
        return processed;
    }
    private String cleanText(String text) {
        String noHtml = text.replaceAll("<[^>]*>", " ");
        return noHtml.replaceAll("[^a-z0-9_]", " ");
    }
    private Set<String> getStopWords() {
        return new HashSet<>(Arrays.asList(
                "the", "is", "are", "was", "were", "and", "or", "but", "if", "of", "to", "in", "on", "at",
                "for", "with", "about", "by", "as", "it", "this", "that", "these", "those", "can", "could",
                "would", "should", "have", "has", "had", "do", "does", "did", "not", "so", "be", "been",
                // Java 关键字 (如果不想统计 public class 这种词)
                "public", "private", "protected", "class", "interface", "void", "return", "static", "final",
                "new", "import", "package", "try", "catch", "throw", "throws", "extends", "implements",
                // 上下文噪音词
                "code", "java", "problem", "issue", "question", "want", "need", "help", "using", "example",
                "output", "error", "exception", "run", "running", "thread", "threads" // "thread" 太泛滥，通常也建议去掉
        ));
    }
    private String formatResult(Number val1, Number val2) {
        if (val1 instanceof Double || val2 instanceof Double) {
            return String.format("%.2f_%.2f", val1.doubleValue(), val2.doubleValue());
        }
        return val1 + "_" + val2;
    }
    private boolean inTopTags(Question question, Set<String> topTags) {
        return question.getTags().stream()
                .filter(t -> topTags.contains(t.getName()))
                .count() >= 2;
    }
    private long countTagsinTime(List<Question> questions) {
        long count = 0;
        for (Question q : questions) {
            List<Map.Entry<String, Integer>> topTagsEntries = getTopNTagsmonthly(q.getDateTime(), 10);
            Set<String> topTagNames = topTagsEntries.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (inTopTags(q, topTagNames)) {
                count++;
            }
        }
        return count;
    }
    private double calculateAvgTagCount(List<Question> questions) {
        return questions.stream()
                .mapToInt(q -> q.getTags() == null ? 0 : q.getTags().size())
                .average()
                .orElse(0.0);
    }
    private double calculateAvgWordCount(List<Question> questions) {
        return questions.stream()
                .mapToInt(q -> {
                    String content = q.getContent();
                    if (content == null || content.isEmpty()) return 0;
                    return content.trim().split("\\s+").length;
                })
                .average()
                .orElse(0.0);
    }
    private double calculateAvgAnswererReputation(List<Question> questions) {
        return questions.stream()
                .mapToDouble(q -> {
                    if (q.getAnswers() == null || q.getAnswers().isEmpty()) return 0.0;
                    return q.getAnswers().stream()
                            .mapToInt(a -> a.getAnswerer() == null ? 0 : a.getAnswerer().getReputation())
                            .average()
                            .orElse(0.0);
                })
                .filter(avg -> avg != 0)
                .average()
                .orElse(0.0);
    }
    private double calculateAvgAnswerScore(List<Question> questions) {
        return questions.stream()
                .mapToDouble(q -> {
                    if (q.getAnswers() == null || q.getAnswers().isEmpty()) return 0.0;
                    return q.getAnswers().stream()
                            .mapToInt(Answer::getScore)
                            .average()
                            .orElse(0.0);
                })
                .filter(avg -> avg != 0)
                .average()
                .orElse(0.0);
    }
}