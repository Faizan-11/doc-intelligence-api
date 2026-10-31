
package com.documind.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    public DocumentRetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(
            String query,
            int topK,
            UUID userId
    ) {
        validateSearchParameters(query, topK, userId);

        FilterExpressionBuilder filterBuilder =
                new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(query.trim())
                .topK(topK)
                .similarityThresholdAll()
                .filterExpression(
                        filterBuilder
                                .eq("userId", userId.toString())
                                .build()
                )
                .build();

        return vectorStore.similaritySearch(request);
    }

    public List<Document> searchByDocument(
            String query,
            int topK,
            UUID documentId,
            UUID userId
    ) {
        validateSearchParameters(query, topK, userId);

        if (documentId == null) {
            throw new IllegalArgumentException(
                    "Document ID must not be null."
            );
        }

        FilterExpressionBuilder filterBuilder =
                new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(query.trim())
                .topK(topK)
                .similarityThresholdAll()
                .filterExpression(
                        filterBuilder.and(
                                filterBuilder.eq(
                                        "userId",
                                        userId.toString()
                                ),
                                filterBuilder.eq(
                                        "documentId",
                                        documentId.toString()
                                )
                        ).build()
                )
                .build();

        return vectorStore.similaritySearch(request);
    }

    private void validateSearchParameters(
            String query,
            int topK,
            UUID userId
    ) {
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

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID must not be null."
            );
        }
    }
}
