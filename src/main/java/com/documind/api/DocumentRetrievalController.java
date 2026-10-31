
package com.documind.api;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.security.AuthenticatedUserService;
import com.documind.api.service.DocumentRetrievalService;

@RestController
@RequestMapping("/api/retrieval")
public class DocumentRetrievalController {

    private final DocumentRetrievalService documentRetrievalService;
    private final AuthenticatedUserService authenticatedUserService;

    public DocumentRetrievalController(
            DocumentRetrievalService documentRetrievalService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.documentRetrievalService = documentRetrievalService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        validateParameters(query, topK);

        UUID userId = authenticatedUserService.getCurrentUserId();

        return documentRetrievalService.search(
                query,
                topK,
                userId
        );
    }

    @GetMapping("/documents/{documentId}/search")
    public List<Document> searchByDocument(
            @PathVariable UUID documentId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        validateParameters(query, topK);

        UUID userId = authenticatedUserService.getCurrentUserId();

        return documentRetrievalService.searchByDocument(
                query,
                topK,
                documentId,
                userId
        );
    }

    private void validateParameters(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query must not be blank."
            );
        }

        if (topK < 1 || topK > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "topK must be between 1 and 20."
            );
        }
    }
}
