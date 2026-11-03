package com.oisin.qaagent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record TestCaseReviewRequest(
    @NotNull(message = "Version is required.")
    @PositiveOrZero(message = "Version must not be negative.")
    Long version,

    @NotNull(message = "Status is required.")
    @Pattern(
        regexp = "APPROVED|REJECTED",
        message = "Status must be APPROVED or REJECTED."
    )
    String status
) {
}
