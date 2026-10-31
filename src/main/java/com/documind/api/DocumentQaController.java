
package com.documind.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.security.AuthenticatedUserService;
import com.documind.api.service.DocumentQaService;

@RestController
@RequestMapping("/api/qa")
public class DocumentQaController {

    private final DocumentQaService documentQaService;
    private final AuthenticatedUserService authenticatedUserService;

    public DocumentQaController(
            DocumentQaService documentQaService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.documentQaService = documentQaService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/ask")
    public DocumentQaService.QaResponse ask(
            @RequestBody AskRequest request
    ) {
        validateRequest(request);

        int topK = request.topK() == null ? 5 : request.topK();

        validateTopK(topK);

        UUID userId = authenticatedUserService.getCurrentUserId();

        try {
            return documentQaService.ask(
                    request.question(),
                    topK,
                    userId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    @PostMapping("/documents/{documentId}/ask")
    public DocumentQaService.QaResponse askByDocument(
            @PathVariable UUID documentId,
            @RequestBody AskRequest request
    ) {
        validateRequest(request);

        int topK = request.topK() == null ? 5 : request.topK();

        validateTopK(topK);

        UUID userId = authenticatedUserService.getCurrentUserId();

        try {
            return documentQaService.askByDocument(
                    request.question(),
                    topK,
                    documentId,
                    userId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    private void validateRequest(AskRequest request) {
        if (request == null
                || request.question() == null
                || request.question().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question must not be blank."
            );
        }
    }

    private void validateTopK(int topK) {
        if (topK < 1 || topK > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "topK must be between 1 and 20."
            );
        }
    }

    public record AskRequest(
            String question,
            Integer topK
    ) {
    }
}
