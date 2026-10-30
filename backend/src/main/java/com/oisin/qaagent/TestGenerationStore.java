package com.oisin.qaagent;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TestGenerationStore {

    private final RequirementRepository requirementRepository;
    private final TestGenerationRepository generationRepository;
    private final String modelName;

    public TestGenerationStore(
        RequirementRepository requirementRepository,
        TestGenerationRepository generationRepository,
        @Value("${gemini.model:}") String modelName
    ) {
        this.requirementRepository = requirementRepository;
        this.generationRepository = generationRepository;
        this.modelName = modelName.trim();
    }

    @Transactional
    public SavedGenerationResponse save(
        Long requirementId,
        DraftTestCases drafts
    ) {
        Requirement requirement = requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Requirement not found."
            ));

        TestGeneration generation =
            new TestGeneration(requirement, modelName, drafts);

        generationRepository.saveAndFlush(generation);

        return SavedGenerationResponse.from(generation);
    }

    @Transactional(readOnly = true)
    public List<SavedGenerationResponse> findByRequirement(Long requirementId) {
        if (!requirementRepository.existsById(requirementId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Requirement not found."
            );
        }

        return generationRepository
            .findByRequirement_IdOrderByIdDesc(requirementId)
            .stream()
            .map(SavedGenerationResponse::from)
            .toList();
    }
}