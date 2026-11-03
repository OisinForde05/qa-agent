package com.oisin.qaagent;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oisin.qaagent.SavedGenerationResponse.TestCaseResponse;

@RestController
@RequestMapping("/api/test-cases")
public class TestCaseReviewController {

    private final TestCaseReviewService reviewService;

    public TestCaseReviewController(TestCaseReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PutMapping("/{id}")
    public TestCaseResponse update(
        @PathVariable("id") Long id,
        @Valid @RequestBody TestCaseUpdateRequest request
    ) {
        return reviewService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TestCaseResponse review(
        @PathVariable("id") Long id,
        @Valid @RequestBody TestCaseReviewRequest request
    ) {
        return reviewService.review(id, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null
            ? "The request could not be completed."
            : exception.getReason();

        return ResponseEntity.status(exception.getStatusCode())
            .body(Map.of("message", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining(" "));

        return ResponseEntity.badRequest()
            .body(Map.of("message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableBody() {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "The request contains invalid JSON or field types."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> handleConcurrentUpdate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "message",
                "This test case has changed. Refresh the page before trying again."
            ));
    }
}
