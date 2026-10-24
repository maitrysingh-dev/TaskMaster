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

/** Metadata for an uploaded file. The bytes themselves live on disk under {@code storedName}. */
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, unique = true)
    private String storedName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    protected Attachment() {
    }

    public Attachment(Task task, User uploadedBy, String originalFileName, String storedName,
                      String contentType, long fileSize) {
        this.task = task;
        this.uploadedBy = uploadedBy;
        this.originalFileName = originalFileName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
