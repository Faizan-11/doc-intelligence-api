
package com.documind.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.service.DocumentQaService;

@RestController
@RequestMapping("/api/qa")
public class DocumentQaController {

    private final DocumentQaService documentQaService;

    public DocumentQaController(
            DocumentQaService documentQaService
    ) {
        this.documentQaService = documentQaService;
    }

    @PostMapping("/ask")
    public DocumentQaService.QaResponse ask(
            @RequestBody AskRequest request
    ) {
        if (request == null
                || request.question() == null
                || request.question().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question must not be blank."
            );
        }

        int topK = request.topK() == null ? 5 : request.topK();

        if (topK < 1 || topK > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "topK must be between 1 and 20."
            );
        }

        try {
            return documentQaService.ask(
                    request.question(),
                    topK
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    public record AskRequest(
            String question,
            Integer topK
    ) {
    }
}
