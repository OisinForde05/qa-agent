package com.oisin.qaagent;

import java.time.Instant;
import java.util.List;

public record SavedGenerationResponse(
    Long id,
    Long requirementId,
    Instant createdAt,
    String modelName,
    String status,
    List<TestCaseResponse> testCases,
    List<String> requirementIssues
) {

    public static SavedGenerationResponse from(TestGeneration generation) {
        List<TestCaseResponse> cases = generation.getTestCases()
            .stream()
            .map(TestCaseResponse::from)
            .toList();

        return new SavedGenerationResponse(
            generation.getId(),
            generation.getRequirement().getId(),
            generation.getCreatedAt(),
            generation.getModelName(),
            "DRAFT",
            cases,
            List.copyOf(generation.getRequirementIssues())
        );
    }

    public record TestCaseResponse(
        Long id,
        String title,
        List<String> preconditions,
        List<String> steps,
        String expectedResult,
        String status
    ) {

        public static TestCaseResponse from(GeneratedTestCase testCase) {
            return new TestCaseResponse(
                testCase.getId(),
                testCase.getTitle(),
                List.copyOf(testCase.getPreconditions()),
                List.copyOf(testCase.getSteps()),
                testCase.getExpectedResult(),
                testCase.getStatus()
            );
        }
    }
}