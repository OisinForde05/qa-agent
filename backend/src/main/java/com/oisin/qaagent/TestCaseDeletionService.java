package com.oisin.qaagent;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TestCaseDeletionService {

    private final GeneratedTestCaseRepository repository;

    public TestCaseDeletionService(GeneratedTestCaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void delete(Long id) {
        GeneratedTestCase testCase = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test case not found."));

        repository.delete(testCase);
        repository.flush();
    }
}