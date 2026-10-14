package com.oisin.qaagent;

public record RequirementResponse(
        Long id,
        String title,
        String description,
        String acceptanceCriteria
) {

    public static RequirementResponse from(Requirement requirement) {
        return new RequirementResponse(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getAcceptanceCriteria()
        );
    }
}