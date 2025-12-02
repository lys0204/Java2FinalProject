package org.example.stackflowanalysis.Repositories;

import jdk.dynalink.linker.LinkerServices;
import org.example.stackflowanalysis.DataStorage.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    /**
     * 1. 针对 Topic Trends [cite: 47] 和 Co-occurrence [cite: 52]
     * 获取所有问题，并一次性抓取 Tags，避免 N+1 查询问题。
     * 数据量：~1000条，可以直接全量拉取到内存处理。
     */
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.tags")
    List<Question> findAllWithTags();

    /**
     * 2. 针对 Topic Trends [cite: 49]
     * 获取特定时间段内、包含特定 Tag 的问题。
     */
    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t " +
            "WHERE t.name = :tagName AND q.dateTime BETWEEN :startDate AND :endDate")
    List<Question> findByTagAndDateRange(
            @Param("tagName") String tagName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 3. 针对 Common Pitfalls (多线程陷阱) [cite: 56, 60]
     * 查找包含特定标签（如 java, multithreading, concurrency）的问题。
     * 关键：使用 JOIN FETCH 同时加载 Answers 和 Tags，因为我们需要分析 body 内容。
     */
    @Query("SELECT DISTINCT q FROM Question q " +
            "LEFT JOIN FETCH q.tags " +
            "LEFT JOIN FETCH q.answers " +
            "JOIN q.tags t " +
            "WHERE t.name IN :tagNames")
    List<Question> findByTagNamesWithContent(@Param("tagNames") List<String> tagNames);

    /**
     * 4. 针对 Solvable vs Hard-to-Solve [cite: 66, 67]
     * 获取所有问题及其回答，用于计算解决率、回答分数等。
     * 同样需要 Fetch Answers 以便计算回答的点赞数。
     */
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answers")
    List<Question> findAllWithAnswers();

    boolean existsById(Long id);
}
