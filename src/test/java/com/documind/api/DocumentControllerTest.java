
package com.documind.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.entity.Document;
import com.documind.api.repository.DocumentRepository;
import com.documind.api.service.FileStorageService;
import com.documind.api.service.TextExtractionService;
import com.documind.api.service.DocumentIngestionService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private final DocumentRepository documentRepository =
            mock(DocumentRepository.class);

    private final FileStorageService fileStorageService =
            mock(FileStorageService.class);

    private final TextExtractionService textExtractionService =
            mock(TextExtractionService.class);
            
    private final DocumentIngestionService documentIngestionService =
        mock(DocumentIngestionService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DocumentController(
                    documentRepository,
                    fileStorageService,
                    textExtractionService,
                    documentIngestionService
            ))
            .build();


    @Test
    void searchReturnsMatchingDocuments() throws Exception {
        Document document = new Document(
                "sample.pdf",
                833L,
                "application/pdf",
                "sample-storage.pdf"
        );
        document.setExtractedText("DocuMind PDF extraction test.");

        when(documentRepository.findByExtractedTextContainingIgnoreCase(
                "PDF extraction"
        )).thenReturn(List.of(document));

        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "PDF extraction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("sample.pdf"))
                .andExpect(jsonPath("$[0].extractedText")
                        .value("DocuMind PDF extraction test."));

        verify(documentRepository)
                .findByExtractedTextContainingIgnoreCase("PDF extraction");
    }

    @Test
    void searchRejectsBlankKeyword() throws Exception {
        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsEmptyListWhenNoDocumentsMatch() throws Exception {
        when(documentRepository.findByExtractedTextContainingIgnoreCase(
                anyString()
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "no-such-term"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
