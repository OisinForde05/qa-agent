package com.oisin.qaagent;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequirementController {

    @PostMapping("/api/requirements")
    public ResponseEntity<?> submit(
            @Valid @RequestBody RequirementRequest request,
            BindingResult validation) {

        if (validation.hasErrors()) {
            String message = validation.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(" "));

            return ResponseEntity.badRequest()
                    .body(Map.of("message", message));
        }

        RequirementRequest accepted = new RequirementRequest(
                request.title().strip(),
                request.description().strip(),
                request.acceptanceCriteria().strip()
        );

        return ResponseEntity.ok(accepted);
    }
}