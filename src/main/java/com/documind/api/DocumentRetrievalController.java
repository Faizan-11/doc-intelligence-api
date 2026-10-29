
package com.documind.api;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.service.DocumentRetrievalService;

@RestController
@RequestMapping("/api/retrieval")
public class DocumentRetrievalController {

    private final DocumentRetrievalService documentRetrievalService;

    public DocumentRetrievalController(
            DocumentRetrievalService documentRetrievalService
    ) {
        this.documentRetrievalService = documentRetrievalService;
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        if (query.isBlank()) {
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

        return documentRetrievalService.search(query, topK);
    }
}
