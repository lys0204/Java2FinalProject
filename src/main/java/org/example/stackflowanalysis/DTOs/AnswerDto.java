package org.example.stackflowanalysis.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record AnswerDto(
        @JsonProperty("answer_id") Long answerId,
        @JsonProperty("body") String body,
        @JsonProperty("creation_date") Long creationDate,
        @JsonProperty("score") int score,
        @JsonProperty("is_accepted") boolean isAccepted,
        OwnerDto owner
) {
    public LocalDateTime getCreationDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(creationDate), ZoneId.of("UTC"));
    }
}
