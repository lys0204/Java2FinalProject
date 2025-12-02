package org.example.stackflowanalysis.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OwnerDto(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("display_name") String displayName
) {}
