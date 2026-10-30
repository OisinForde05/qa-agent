package com.oisin.qaagent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_generations")
public class TestGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @ElementCollection
    @CollectionTable(
        name = "generation_issues",
        joinColumns = @JoinColumn(name = "generation_id")
    )
    @OrderColumn(name = "issue_position")
    @Column(name = "issue_text", nullable = false, columnDefinition = "text")
    private List<String> requirementIssues = new ArrayList<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<GeneratedTestCase> testCases = new ArrayList<>();

    protected TestGeneration() {
    }

    public TestGeneration(
        Requirement requirement,
        String modelName,
        DraftTestCases drafts
    ) {
        this.requirement = requirement;
        this.modelName = modelName;
        this.createdAt = Instant.now();
        this.requirementIssues.addAll(drafts.requirementIssues());

        for (DraftTestCases.TestCase draft : drafts.testCases()) {
            this.testCases.add(new GeneratedTestCase(this, draft));
        }
    }

    public Long getId() {
        return id;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getModelName() {
        return modelName;
    }

    public List<String> getRequirementIssues() {
        return requirementIssues;
    }

    public List<GeneratedTestCase> getTestCases() {
        return testCases;
    }
}