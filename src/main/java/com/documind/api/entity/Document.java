
package com.documind.api.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 255)
    private String contentType;

    @Column(nullable = false, unique = true, length = 255)
    private String storageFileName;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'PENDING'"
    )
    private IngestionStatus ingestionStatus = IngestionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String ingestionError;

    @JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User owner;

    protected Document() {
        // Required by JPA.
    }

    public Document(
            String fileName,
            long fileSize,
            String contentType,
            String storageFileName
    ) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.storageFileName = storageFileName;
        this.uploadedAt = Instant.now();
        this.ingestionStatus = IngestionStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageFileName() {
        return storageFileName;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public IngestionStatus getIngestionStatus() {
        return ingestionStatus;
    }

    public void setIngestionStatus(IngestionStatus ingestionStatus) {
        this.ingestionStatus = ingestionStatus;
    }

    public String getIngestionError() {
        return ingestionError;
    }

    public void setIngestionError(String ingestionError) {
        this.ingestionError = ingestionError;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
