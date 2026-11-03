package com.oisin.qaagent;

import java.time.Instant;
import java.util.List;

public record SavedGenerationResponse(
    Long id,
    Long requirementId,
    Instant createdAt,
    String modelName,
    List<TestCaseResponse> testCases,
    List<String> requirementIssues
) {

    public SavedGenerationResponse(
        Long id,
        Long requirementId,
        Instant createdAt,
        String modelName,
        String status,
        List<TestCaseResponse> testCases,
        List<String> requirementIssues
    ) {
        this(id, requirementId, createdAt, modelName, testCases, requirementIssues);
    }

    public String status() {
        return testCases.isEmpty() ? "DRAFT" : testCases.getFirst().status();
    }

    public static SavedGenerationResponse from(TestGeneration generation) {
        return new SavedGenerationResponse(
            generation.getId(),
            generation.getRequirement().getId(),
            generation.getCreatedAt(),
            generation.getModelName(),
            generation.getTestCases()
                .stream()
                .map(TestCaseResponse::from)
                .toList(),
            List.copyOf(generation.getRequirementIssues())
        );
    }

    public record TestCaseResponse(
        Long id,
        Long version,
        String title,
        List<String> preconditions,
        List<String> steps,
        String expectedResult,
        String status
    ) {

        public static TestCaseResponse from(GeneratedTestCase testCase) {
            return new TestCaseResponse(
                testCase.getId(),
                testCase.getVersion(),
                testCase.getTitle(),
                List.copyOf(testCase.getPreconditions()),
                List.copyOf(testCase.getSteps()),
                testCase.getExpectedResult(),
                testCase.getStatus()
            );
        }
    }
}