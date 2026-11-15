package com.oisin.qaagent;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/requirements")
public class RequirementDeletionController {

    private final RequirementDeletionService deletionService;

    public RequirementDeletionController(
            RequirementDeletionService deletionService) {
        this.deletionService = deletionService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        deletionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatusException(
            ResponseStatusException exception) {
        String message = exception.getReason();

        if (message == null) {
            message = "The requirement could not be deleted.";
        }

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<Map<String, String>> handleDeletionConflict() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        "The requirement could not be deleted because of a data conflict. "
                                + "Refresh and try again."));
    }
}