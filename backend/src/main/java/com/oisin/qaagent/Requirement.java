package com.oisin.qaagent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "requirements")
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(
            name = "acceptance_criteria",
            nullable = false,
            columnDefinition = "text"
    )
    private String acceptanceCriteria;

    protected Requirement() {
    }

    public Requirement(
            String title,
            String description,
            String acceptanceCriteria) {
        this.title = title;
        this.description = description;
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }
}