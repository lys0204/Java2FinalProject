package org.example.stackflowanalysis.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record QuestionDto(
        @JsonProperty("question_id") Long questionId,
        String title,
        @JsonProperty("body") String body, // 需要在API请求中开启 filter=!withbody
        @JsonProperty("creation_date") Long creationDate,
        @JsonProperty("score") int score,
        @JsonProperty("view_count") int viewCount,
        @JsonProperty("answer_count") int answerCount,
        @JsonProperty("is_answered") boolean isAnswered,
        List<String> tags,
        OwnerDto owner,
        List<AnswerDto> answers
) {
    public LocalDateTime getCreationDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(creationDate), ZoneId.of("UTC"));
    }
}
