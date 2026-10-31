
package com.documind.api;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.documind.api.entity.Document;
import com.documind.api.entity.IngestionStatus;
import com.documind.api.entity.User;
import com.documind.api.repository.DocumentRepository;
import com.documind.api.security.AuthenticatedUserService;
import com.documind.api.service.DocumentIngestionService;
import com.documind.api.service.FileStorageService;
import com.documind.api.service.TextExtractionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private final DocumentRepository documentRepository =
            mock(DocumentRepository.class);

    private final FileStorageService fileStorageService =
            mock(FileStorageService.class);

    private final TextExtractionService textExtractionService =
            mock(TextExtractionService.class);

    private final DocumentIngestionService documentIngestionService =
            mock(DocumentIngestionService.class);

    private final AuthenticatedUserService authenticatedUserService =
            mock(AuthenticatedUserService.class);

    private final User authenticatedUser = mock(User.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DocumentController(
                    documentRepository,
                    fileStorageService,
                    textExtractionService,
                    documentIngestionService,
                    authenticatedUserService
            ))
            .build();

    @BeforeEach
    void setUp() {
        when(authenticatedUserService.getCurrentUser())
                .thenReturn(authenticatedUser);

        when(authenticatedUserService.getCurrentUserId())
                .thenReturn(TEST_USER_ID);

        when(authenticatedUser.getId())
                .thenReturn(TEST_USER_ID);
    }

    @Test
    void uploadPreservesDocumentWhenIngestionFails() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "upload.txt",
                "text/plain",
                "DocuMind ingestion failure verification."
                        .getBytes(StandardCharsets.UTF_8)
        );

        when(fileStorageService.store(any(MultipartFile.class)))
                .thenReturn("stored-upload.txt");

        when(fileStorageService.resolve("stored-upload.txt"))
                .thenReturn(Path.of("/tmp/stored-upload.txt"));

        when(textExtractionService.extractText(
                Path.of("/tmp/stored-upload.txt"),
                "upload.txt"
        )).thenReturn("DocuMind ingestion failure verification.");

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Test embedding API failure"))
                .when(documentIngestionService)
                .ingest(any(Document.class), anyString());

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("upload.txt"))
                .andExpect(jsonPath("$.ingestionStatus").value("FAILED"))
                .andExpect(jsonPath("$.ingestionError")
                        .value("Test embedding API failure"));

        // The document must be saved, including its failure status.
        verify(documentRepository, times(2))
                .save(any(Document.class));

        // The original uploaded file must not be deleted.
        verify(fileStorageService, never())
                .delete("stored-upload.txt");

        // The document record must not be deleted.
        verify(documentRepository, never())
                .delete(any(Document.class));
    }

    @Test
    void searchReturnsMatchingDocuments() throws Exception {

        Document document = new Document(
                "sample.pdf",
                833L,
                "application/pdf",
                "sample-storage.pdf"
        );

        document.setExtractedText("DocuMind PDF extraction test.");

        when(documentRepository
                .findByExtractedTextContainingIgnoreCaseAndOwner_Id(
                        "PDF extraction",
                        TEST_USER_ID
                )).thenReturn(List.of(document));

        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "PDF extraction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("sample.pdf"))
                .andExpect(jsonPath("$[0].extractedText")
                        .value("DocuMind PDF extraction test."));

        verify(documentRepository)
                .findByExtractedTextContainingIgnoreCaseAndOwner_Id(
                        "PDF extraction",
                        TEST_USER_ID
                );
    }

    @Test
    void searchRejectsBlankKeyword() throws Exception {

        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsEmptyListWhenNoDocumentsMatch() throws Exception {

        when(documentRepository
                .findByExtractedTextContainingIgnoreCaseAndOwner_Id(
                        anyString(),
                        any(UUID.class)
                )).thenReturn(List.of());

        mockMvc.perform(get("/api/documents/search")
                        .param("keyword", "no-such-term"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void retrySuccessfullyCompletesDocument() throws Exception {

        Document document = createDocumentWithText();

        when(documentRepository.findByIdAndOwner_Id(
                document.getId(),
                TEST_USER_ID
        )).thenReturn(Optional.of(document));

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            Document target = invocation.getArgument(0);
            target.setIngestionStatus(IngestionStatus.COMPLETED);
            return null;
        }).when(documentIngestionService)
                .ingest(document, document.getExtractedText());

        mockMvc.perform(
                org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders
                        .post("/api/documents/{id}/retry", document.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestionStatus")
                        .value("COMPLETED"));

        verify(documentIngestionService)
                .ingest(document, document.getExtractedText());
    }

    @Test
    void retryMarksDocumentFailedWhenIngestionThrows() throws Exception {

        Document document = createDocumentWithText();

        when(documentRepository.findByIdAndOwner_Id(
                document.getId(),
                TEST_USER_ID
        )).thenReturn(Optional.of(document));

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Test embedding failure"))
                .when(documentIngestionService)
                .ingest(document, document.getExtractedText());

        mockMvc.perform(
                org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders
                        .post("/api/documents/{id}/retry", document.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestionStatus")
                        .value("FAILED"))
                .andExpect(jsonPath("$.ingestionError")
                        .value("Test embedding failure"));
    }

    @Test
    void retryRejectsDocumentWithoutExtractedText() throws Exception {

        Document document = new Document(
                "empty.pdf",
                100L,
                "application/pdf",
                "empty-storage.pdf"
        );

        assignDocumentId(document);

        when(documentRepository.findByIdAndOwner_Id(
                document.getId(),
                TEST_USER_ID
        )).thenReturn(Optional.of(document));

        mockMvc.perform(
                org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders
                        .post("/api/documents/{id}/retry", document.getId())
        )
                .andExpect(status().isUnprocessableEntity());

        verify(documentIngestionService, never())
                .ingest(any(Document.class), anyString());
    }

    private Document createDocumentWithText() {

        Document document = new Document(
                "sample.txt",
                48L,
                "text/plain",
                "sample-storage.txt"
        );

        document.setExtractedText(
                "DocuMind stores documents for semantic search."
        );

        assignDocumentId(document);

        return document;
    }

    private void assignDocumentId(Document document) {

        try {
            var idField = Document.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(document, UUID.randomUUID());

        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to assign test document ID",
                    exception
            );
        }
    }
}
