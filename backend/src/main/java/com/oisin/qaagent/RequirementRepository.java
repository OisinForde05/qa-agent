package com.oisin.qaagent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository
        extends JpaRepository<Requirement, Long> {
}