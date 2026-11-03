package com.oisin.qaagent;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.oisin.qaagent.SavedGenerationResponse.TestCaseResponse;

@Service
public class TestCaseReviewService {

    private final GeneratedTestCaseRepository repository;

    public TestCaseReviewService(GeneratedTestCaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TestCaseResponse update(Long id, TestCaseUpdateRequest request) {
        GeneratedTestCase testCase = findCurrent(id, request.version());

        testCase.updateDetails(
            request.title(),
            request.preconditions(),
            request.steps(),
            request.expectedResult()
        );

        repository.saveAndFlush(testCase);

        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse review(Long id, TestCaseReviewRequest request) {
        GeneratedTestCase testCase = findCurrent(id, request.version());

        switch (request.status()) {
            case "APPROVED" -> testCase.approve();
            case "REJECTED" -> testCase.reject();
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Status must be APPROVED or REJECTED."
            );
        }

        repository.saveAndFlush(testCase);

        return TestCaseResponse.from(testCase);
    }

    private GeneratedTestCase findCurrent(Long id, Long version) {
        GeneratedTestCase testCase = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Test case not found."
            ));

        if (!Objects.equals(testCase.getVersion(), version)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This test case has changed. Refresh the page before trying again."
            );
        }

        return testCase;
    }
}
