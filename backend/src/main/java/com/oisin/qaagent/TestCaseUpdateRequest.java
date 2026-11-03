package com.oisin.qaagent;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TestCaseUpdateRequest(
    @NotNull(message = "Version is required.")
    @PositiveOrZero(message = "Version must not be negative.")
    Long version,

    @NotBlank(message = "Title is required.")
    String title,

    @NotNull(message = "Preconditions must be supplied as a list.")
    List<@NotBlank(message = "Preconditions cannot contain blank items.") String>
        preconditions,

    @NotEmpty(message = "At least one step is required.")
    List<@NotBlank(message = "Steps cannot contain blank items.") String>
        steps,

    @NotBlank(message = "Expected result is required.")
    String expectedResult
) {
}
