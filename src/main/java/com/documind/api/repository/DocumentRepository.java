
package com.documind.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.documind.api.entity.Document;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {
}
