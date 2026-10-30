package com.oisin.qaagent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestGenerationRepository
        extends JpaRepository<TestGeneration, Long> {

    List<TestGeneration> findByRequirement_IdOrderByIdDesc(Long requirementId);
}