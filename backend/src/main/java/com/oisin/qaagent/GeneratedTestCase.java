package com.oisin.qaagent;

import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_test_cases")
public class GeneratedTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false)
    private TestGeneration generation;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "expected_result", nullable = false, columnDefinition = "text")
    private String expectedResult;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @ElementCollection
    @CollectionTable(
        name = "test_case_preconditions",
        joinColumns = @JoinColumn(name = "test_case_id")
    )
    @OrderColumn(name = "item_position")
    @Column(name = "item_text", nullable = false, columnDefinition = "text")
    private List<String> preconditions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "test_case_steps",
        joinColumns = @JoinColumn(name = "test_case_id")
    )
    @OrderColumn(name = "item_position")
    @Column(name = "item_text", nullable = false, columnDefinition = "text")
    private List<String> steps = new ArrayList<>();

    protected GeneratedTestCase() {
    }

    public GeneratedTestCase(
        TestGeneration generation,
        DraftTestCases.TestCase draft
    ) {
        this.generation = generation;
        this.title = draft.title();
        this.expectedResult = draft.expectedResult();
        this.preconditions.addAll(draft.preconditions());
        this.steps.addAll(draft.steps());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getPreconditions() {
        return preconditions;
    }

    public List<String> getSteps() {
        return steps;
    }
}