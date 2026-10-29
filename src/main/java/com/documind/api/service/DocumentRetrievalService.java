
package com.documind.api.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    public DocumentRetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must not be blank."
            );
        }

        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException(
                    "topK must be between 1 and 20."
            );
        }

        SearchRequest request = SearchRequest.builder()
                .query(query.trim())
                .topK(topK)
                .similarityThresholdAll()
                .build();

        return vectorStore.similaritySearch(request);
    }
}
