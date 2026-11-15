package com.oisin.qaagent;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RequirementDeletionService {

    private final RequirementRepository requirementRepository;
    private final TestGenerationRepository generationRepository;

    public RequirementDeletionService(
            RequirementRepository requirementRepository,
            TestGenerationRepository generationRepository) {
        this.requirementRepository = requirementRepository;
        this.generationRepository = generationRepository;
    }

    @Transactional
    public void delete(Long requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Requirement not found."));

        var generations = generationRepository
                .findByRequirement_IdOrderByIdDesc(requirementId);

        generationRepository.deleteAll(generations);
        generationRepository.flush();

        requirementRepository.delete(requirement);
        requirementRepository.flush();
    }
}