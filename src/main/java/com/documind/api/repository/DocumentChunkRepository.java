
package com.documind.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.documind.api.entity.DocumentChunk;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(
            UUID documentId
    );

    @Transactional
    void deleteByDocument_Id(UUID documentId);
}
