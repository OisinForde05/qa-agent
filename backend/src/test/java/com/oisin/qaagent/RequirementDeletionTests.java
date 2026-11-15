package com.oisin.qaagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequirementDeletionTests {

    @Autowired
    private RequirementDeletionController controller;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TestGenerationRepository generationRepository;

    @Autowired
    private GeneratedTestCaseRepository testCaseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void deletesRequirementAndAllRelatedData() throws Exception {
        Requirement target = createRequirement("Requirement to delete");
        Requirement other = createRequirement("Requirement to keep");

        TestGeneration first = createGeneration(target);
        TestGeneration second = createGeneration(target);
        TestGeneration retained = createGeneration(other);

        Long targetId = target.getId();
        Long otherId = other.getId();
        Long retainedGenerationId = retained.getId();
        Long retainedCaseId = retained.getTestCases().get(0).getId();

        List<Long> generationIds = List.of(first.getId(), second.getId());

        List<Long> caseIds = List.of(
                first.getTestCases().get(0).getId(),
                second.getTestCases().get(0).getId());

        for (Long generationId : generationIds) {
            assertEquals(1, count(
                    "SELECT COUNT(*) FROM generation_issues WHERE generation_id = ?",
                    generationId));
        }

        for (Long caseId : caseIds) {
            assertEquals(1, count(
                    "SELECT COUNT(*) FROM test_case_preconditions WHERE test_case_id = ?",
                    caseId));

            assertEquals(2, count(
                    "SELECT COUNT(*) FROM test_case_steps WHERE test_case_id = ?",
                    caseId));
        }

        mockMvc.perform(delete("/api/requirements/{id}", targetId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();
        entityManager.clear();

        assertFalse(requirementRepository.existsById(targetId));

        for (Long generationId : generationIds) {
            assertFalse(generationRepository.existsById(generationId));

            assertEquals(0, count(
                    "SELECT COUNT(*) FROM generation_issues WHERE generation_id = ?",
                    generationId));

            assertEquals(0, count(
                    "SELECT COUNT(*) FROM generated_test_cases WHERE generation_id = ?",
                    generationId));
        }

        for (Long caseId : caseIds) {
            assertFalse(testCaseRepository.existsById(caseId));

            assertEquals(0, count(
                    "SELECT COUNT(*) FROM test_case_preconditions WHERE test_case_id = ?",
                    caseId));

            assertEquals(0, count(
                    "SELECT COUNT(*) FROM test_case_steps WHERE test_case_id = ?",
                    caseId));
        }

        assertTrue(requirementRepository.existsById(otherId));
        assertTrue(generationRepository.existsById(retainedGenerationId));
        assertTrue(testCaseRepository.existsById(retainedCaseId));

        assertEquals(1, count(
                "SELECT COUNT(*) FROM generation_issues WHERE generation_id = ?",
                retainedGenerationId));

        assertEquals(1, count(
                "SELECT COUNT(*) FROM test_case_preconditions WHERE test_case_id = ?",
                retainedCaseId));

        assertEquals(2, count(
                "SELECT COUNT(*) FROM test_case_steps WHERE test_case_id = ?",
                retainedCaseId));
    }

    @Test
    void deletesRequirementWithoutGenerations() throws Exception {
        Requirement requirement = createRequirement("No generations");
        Long id = requirement.getId();

        mockMvc.perform(delete("/api/requirements/{id}", id))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertFalse(requirementRepository.existsById(id));
    }

    @Test
    void returnsNotFoundWhenRequirementDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/requirements/{id}", -1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Requirement not found."));
    }

    private Requirement createRequirement(String title) {
        return requirementRepository.saveAndFlush(new Requirement(
                title,
                "A user can add a task.",
                "A valid title creates a task."));
    }

    private TestGeneration createGeneration(Requirement requirement) {
        var testCase = new DraftTestCases.TestCase(
                "Add a valid task",
                List.of("The task page is open."),
                List.of(
                        "Enter a valid task title.",
                        "Select Add."),
                "The task appears in the list.");

        var drafts = new DraftTestCases(
                List.of(testCase),
                List.of("The maximum title length is unspecified."));

        return generationRepository.saveAndFlush(
                new TestGeneration(requirement, "test-model", drafts));
    }

    private int count(String sql, Long id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }
}