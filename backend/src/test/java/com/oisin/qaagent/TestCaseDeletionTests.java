package com.oisin.qaagent;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TestCaseDeletionTests {

    @Autowired
    private TestCaseDeletionController controller;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private GeneratedTestCaseRepository testCaseRepository;

    @Autowired
    private TestGenerationRepository generationRepository;

    @Autowired
    private TestGenerationStore generationStore;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void deletesCaseAndItsCollectionsWhilePreservingOtherData() throws Exception {
        Requirement requirement = requirementRepository.saveAndFlush(
                new Requirement(
                        "Add a task",
                        "A user can add a task.",
                        "A valid title creates a task."));

        DraftTestCases drafts = new DraftTestCases(
                List.of(
                        new DraftTestCases.TestCase(
                                "Add a valid task",
                                List.of("The task page is open."),
                                List.of("Enter a title.", "Select Add."),
                                "The task appears."),
                        new DraftTestCases.TestCase(
                                "Reject an empty title",
                                List.of("The task page is open."),
                                List.of("Leave the title empty.", "Select Add."),
                                "No task is created.")
                ),
                List.of()
        );

        SavedGenerationResponse generation =
                generationStore.save(requirement.getId(), drafts);

        Long deletedId = generation.testCases().get(0).id();
        Long remainingId = generation.testCases().get(1).id();

        assertEquals(1L, countPreconditions(deletedId));
        assertEquals(2L, countSteps(deletedId));

        mvc.perform(delete("/api/test-cases/{id}", deletedId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertFalse(testCaseRepository.existsById(deletedId));
        assertEquals(0L, countPreconditions(deletedId));
        assertEquals(0L, countSteps(deletedId));

        assertTrue(testCaseRepository.existsById(remainingId));
        assertEquals(1L, countPreconditions(remainingId));
        assertEquals(2L, countSteps(remainingId));

        assertTrue(requirementRepository.existsById(requirement.getId()));
        assertTrue(generationRepository.existsById(generation.id()));

        SavedGenerationResponse reloaded =
                generationStore.findByRequirement(requirement.getId()).get(0);

        assertEquals(1, reloaded.testCases().size());
        assertEquals(remainingId, reloaded.testCases().get(0).id());

        mvc.perform(delete("/api/test-cases/{id}", deletedId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Test case not found."));

        mvc.perform(delete("/api/test-cases/{id}", remainingId))
                .andExpect(status().isNoContent());

        SavedGenerationResponse emptyGeneration =
                generationStore.findByRequirement(requirement.getId()).get(0);

        assertTrue(emptyGeneration.testCases().isEmpty());
        assertTrue(generationRepository.existsById(generation.id()));
        assertTrue(requirementRepository.existsById(requirement.getId()));
        assertEquals(0L, countPreconditions(remainingId));
        assertEquals(0L, countSteps(remainingId));
    }

    private long countPreconditions(Long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM test_case_preconditions WHERE test_case_id = ?",
                Long.class,
                id);
    }

    private long countSteps(Long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM test_case_steps WHERE test_case_id = ?",
                Long.class,
                id);
    }
}