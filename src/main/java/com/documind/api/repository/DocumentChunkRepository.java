
package com.documind.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.documind.api.entity.DocumentChunk;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(
            UUID documentId
    );

    void deleteByDocument_Id(UUID documentId);
}
