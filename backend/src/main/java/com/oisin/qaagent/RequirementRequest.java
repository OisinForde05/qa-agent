package com.oisin.qaagent;

import jakarta.validation.constraints.NotBlank;

public record RequirementRequest(
        @NotBlank(message = "Title is required.")
        String title,

        @NotBlank(message = "Description is required.")
        String description,

        @NotBlank(message = "Acceptance criteria are required.")
        String acceptanceCriteria
) {
}