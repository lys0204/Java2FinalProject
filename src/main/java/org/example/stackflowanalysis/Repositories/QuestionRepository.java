package org.example.stackflowanalysis.Repositories;

import org.example.stackflowanalysis.Data.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.tags")
    List<Question> findAllWithTags();

    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t " +
            "WHERE t.name = :tagName AND q.dateTime BETWEEN :startDate AND :endDate")
    List<Question> findByTagAndDateRange(
            @Param("tagName") String tagName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT DISTINCT q FROM Question q " +
            "LEFT JOIN FETCH q.tags " +
            "LEFT JOIN FETCH q.answers " +
            "JOIN q.tags t " +
            "WHERE t.name IN :tagNames")
    List<Question> findByTagNamesWithContent(@Param("tagNames") List<String> tagNames);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answers")
    List<Question> findAllWithAnswers();

    boolean existsById(Long id);
}
