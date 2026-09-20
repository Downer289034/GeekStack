package com.community.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 网关入参。question 最长 2000；thinking 仅 enabled/disabled。
 */
public record AiAskRequest(
        @NotBlank @Size(max = 2000) String question,
        String thinking,
        @Min(1) @Max(10) @JsonProperty("top_k") Integer topK) {
}
