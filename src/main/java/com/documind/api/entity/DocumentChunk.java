
package com.documind.api.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    protected DocumentChunk() {
        // Required by JPA.
    }

    public DocumentChunk(
            Document document,
            int chunkIndex,
            String chunkText
    ) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }
}
