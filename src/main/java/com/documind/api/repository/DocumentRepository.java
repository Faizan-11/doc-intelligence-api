
package com.documind.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.documind.api.entity.Document;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    List<Document> findByExtractedTextContainingIgnoreCase(String keyword);
}
