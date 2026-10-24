package com.taskflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** A project doubles as a team: everyone who is a member can see and work on its tasks. */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, unique = true, length = 20)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Project() {
    }

    public Project(String name, String description, String inviteCode, User owner) {
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public User getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
