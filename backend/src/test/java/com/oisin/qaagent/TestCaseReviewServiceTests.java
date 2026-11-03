package com.oisin.qaagent;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TestCaseReviewServiceTests {

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TestGenerationStore generationStore;

    @Autowired
    private TestCaseReviewService reviewService;

    private SavedGenerationResponse createGeneration() {
        Requirement requirement = requirementRepository.save(
            new Requirement(
                "Add a task",
                "A user can add a task.",
                "A valid title creates a task. Empty titles are rejected."
            )
        );

        DraftTestCases drafts = new DraftTestCases(
            List.of(
                new DraftTestCases.TestCase(
                    "Add a task",
                    List.of(),
                    List.of("Enter a title.", "Select Add."),
                    "The task appears."
                ),
                new DraftTestCases.TestCase(
                    "Reject an empty title",
                    List.of(),
                    List.of("Leave the title empty.", "Select Add."),
                    "No task is created."
                )
            ),
            List.of()
        );

        return generationStore.save(requirement.getId(), drafts);
    }

    @Test
    void savesReviewsAndResetsEditedCasesToDraft() {
        var generation = createGeneration();
        var original = generation.testCases().getFirst();

        var approved = reviewService.review(
            original.id(),
            new TestCaseReviewRequest(original.version(), "APPROVED")
        );

        var afterApproval = generationStore
            .findByRequirement(generation.requirementId())
            .getFirst();

        assertEquals("APPROVED", afterApproval.testCases().getFirst().status());

        var edited = reviewService.update(
            original.id(),
            new TestCaseUpdateRequest(
                approved.version(),
                " Updated title ",
                List.of(" Open the task page. "),
                List.of(" Enter Buy groceries. ", " Select Add. "),
                " Buy groceries appears. "
            )
        );

        var afterEdit = generationStore
            .findByRequirement(generation.requirementId())
            .getFirst();

        var saved = afterEdit.testCases().getFirst();

        assertEquals("DRAFT", saved.status());
        assertEquals("Updated title", saved.title());
        assertEquals(
            List.of("Enter Buy groceries.", "Select Add."),
            saved.steps()
        );

        reviewService.review(
            edited.id(),
            new TestCaseReviewRequest(edited.version(), "REJECTED")
        );

        var afterRejection = generationStore
            .findByRequirement(generation.requirementId())
            .getFirst();

        assertEquals("REJECTED", afterRejection.testCases().getFirst().status());
        assertEquals("DRAFT", afterRejection.testCases().get(1).status());
        assertEquals(
            "Reject an empty title",
            afterRejection.testCases().get(1).title()
        );
    }

    @Test
    void rejectsAnUpdateUsingAnOldVersion() {
        var generation = createGeneration();
        var original = generation.testCases().getFirst();

        reviewService.review(
            original.id(),
            new TestCaseReviewRequest(original.version(), "APPROVED")
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> reviewService.update(
                original.id(),
                new TestCaseUpdateRequest(
                    original.version(),
                    "Old browser edit",
                    List.of(),
                    List.of("Select Add."),
                    "A task appears."
                )
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        var saved = generationStore
            .findByRequirement(generation.requirementId())
            .getFirst()
            .testCases()
            .getFirst();

        assertEquals("APPROVED", saved.status());
        assertEquals(original.title(), saved.title());
    }
}
