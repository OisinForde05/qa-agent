package com.oisin.qaagent;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DraftTestCases(
        @NotNull
        @Size(max = 5)
        List<@NotNull @Valid TestCase> testCases,

        @NotNull
        List<@NotBlank String> requirementIssues
) {

    public record TestCase(
            @NotBlank String title,

            @NotNull
            List<@NotBlank String> preconditions,

            @NotEmpty
            List<@NotBlank String> steps,

            @NotBlank String expectedResult
    ) {
    }
}