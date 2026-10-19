package com.oisin.qaagent;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TestGenerationController {

    private final RequirementRepository requirementRepository;
    private final GeminiClient geminiClient;

    public TestGenerationController(
            RequirementRepository requirementRepository,
            GeminiClient geminiClient) {
        this.requirementRepository = requirementRepository;
        this.geminiClient = geminiClient;
    }

    @PostMapping("/api/requirements/{id}/test-cases/generate")
    public ResponseEntity<?> generate(@PathVariable("id") Long id) {
        var requirement = requirementRepository.findById(id);

        if (requirement.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Requirement not found."));
        }

        try {
            DraftTestCases drafts = geminiClient.generate(requirement.get());

            return ResponseEntity.ok(Map.of(
                    "requirementId", id,
                    "status", "DRAFT",
                    "testCases", drafts.testCases(),
                    "requirementIssues", drafts.requirementIssues()
            ));

        } catch (ResponseStatusException exception) {
            String message = exception.getReason() == null
                    ? "Test generation failed."
                    : exception.getReason();

            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("message", message));
        }
    }
}