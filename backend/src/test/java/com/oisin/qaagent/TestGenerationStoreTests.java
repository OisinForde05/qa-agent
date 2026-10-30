package com.oisin.qaagent;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TestGenerationStoreTests {

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TestGenerationStore generationStore;

    @Test
    void savesCasesAndPreservesEarlierGenerations() {
        Requirement requirement = requirementRepository.save(
            new Requirement(
                "Add a task",
                "A user can add a task.",
                "A valid title creates a task."
            )
        );

        DraftTestCases drafts = new DraftTestCases(
            List.of(new DraftTestCases.TestCase(
                "Add a valid task",
                List.of("The task page is open."),
                List.of("Enter Buy groceries.", "Select Add."),
                "Buy groceries appears in the task list."
            )),
            List.of("The maximum title length is unspecified.")
        );

        SavedGenerationResponse first =
            generationStore.save(requirement.getId(), drafts);

        SavedGenerationResponse second =
            generationStore.save(requirement.getId(), drafts);

        List<SavedGenerationResponse> history =
            generationStore.findByRequirement(requirement.getId());

        assertEquals(2, history.size());
        assertEquals(second.id(), history.get(0).id());
        assertEquals(first.id(), history.get(1).id());

        var savedCase = history.get(1).testCases().getFirst();

        assertNotNull(savedCase.id());
        assertEquals("DRAFT", savedCase.status());
        assertEquals(drafts.testCases().getFirst().steps(), savedCase.steps());
        assertEquals(
            drafts.testCases().getFirst().preconditions(),
            savedCase.preconditions()
        );
        assertEquals(drafts.requirementIssues(), history.get(1).requirementIssues());
    }

    @Test
    void savesClarificationQuestionsWithoutTestCases() {
        Requirement requirement = requirementRepository.save(
            new Requirement("test", "test", "test")
        );

        DraftTestCases drafts = new DraftTestCases(
            List.of(),
            List.of("No expected behaviour is specified.")
        );

        generationStore.save(requirement.getId(), drafts);

        var history = generationStore.findByRequirement(requirement.getId());

        assertEquals(1, history.size());
        assertTrue(history.getFirst().testCases().isEmpty());
        assertEquals(
            drafts.requirementIssues(),
            history.getFirst().requirementIssues()
        );
    }
}