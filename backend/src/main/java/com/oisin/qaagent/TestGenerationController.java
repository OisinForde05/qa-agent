package com.oisin.qaagent;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TestGenerationController {

    private final RequirementRepository requirementRepository;
    private final GeminiClient geminiClient;
    private final TestGenerationStore generationStore;

    public TestGenerationController(
        RequirementRepository requirementRepository,
        GeminiClient geminiClient,
        TestGenerationStore generationStore
    ) {
        this.requirementRepository = requirementRepository;
        this.geminiClient = geminiClient;
        this.generationStore = generationStore;
    }

    @PostMapping("/api/requirements/{id}/test-cases/generate")
    public ResponseEntity<?> generate(@PathVariable("id") Long id) {
        var requirement = requirementRepository.findById(id);

        if (requirement.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Requirement not found."));
        }

        try {
            DraftTestCases drafts = geminiClient.generate(requirement.get());

            SavedGenerationResponse saved = generationStore.save(id, drafts);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (ResponseStatusException exception) {
            return errorResponse(exception);
        }
    }

    @GetMapping("/api/requirements/{id}/test-generations")
    public ResponseEntity<?> getGenerations(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(generationStore.findByRequirement(id));
        } catch (ResponseStatusException exception) {
            return errorResponse(exception);
        }
    }

    private ResponseEntity<?> errorResponse(ResponseStatusException exception) {
        String message = exception.getReason() == null
            ? "The request could not be completed."
            : exception.getReason();

        return ResponseEntity.status(exception.getStatusCode())
            .body(Map.of("message", message));
    }
}