package com.oisin.qaagent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequirementController {

    private final RequirementRepository requirementRepository;

    public RequirementController(
            RequirementRepository requirementRepository) {
        this.requirementRepository = requirementRepository;
    }

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

        Requirement requirement = new Requirement(
                request.title().strip(),
                request.description().strip(),
                request.acceptanceCriteria().strip()
        );

        Requirement saved = requirementRepository.save(requirement);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RequirementResponse.from(saved));
    }

    @GetMapping("/api/requirements")
    public List<RequirementResponse> list() {
        return requirementRepository
                .findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(RequirementResponse::from)
                .toList();
    }
}