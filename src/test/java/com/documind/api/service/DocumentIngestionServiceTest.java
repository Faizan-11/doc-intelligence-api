
package com.documind.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import com.documind.api.entity.DocumentChunk;
import com.documind.api.entity.IngestionStatus;
import com.documind.api.entity.User;
import com.documind.api.repository.DocumentChunkRepository;
import com.documind.api.repository.DocumentRepository;

class DocumentIngestionServiceTest {

    private VectorStore vectorStore;
    private TextChunkingService textChunkingService;
    private DocumentChunkRepository chunkRepository;
    private DocumentRepository documentRepository;
    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {

        vectorStore = mock(VectorStore.class);
        textChunkingService = new TextChunkingService();
        chunkRepository = mock(DocumentChunkRepository.class);
        documentRepository = mock(DocumentRepository.class);

        ingestionService = new DocumentIngestionService(
                vectorStore,
                textChunkingService,
                chunkRepository,
                documentRepository
        );
    }

    @Test
    void rejectsNullOrBlankTextAndMarksDocumentFailed() {

        com.documind.api.entity.Document document = createDocument();

        assertThrows(
                IllegalArgumentException.class,
                () -> ingestionService.ingest(document, null)
        );

        assertEquals(
                IngestionStatus.FAILED,
                document.getIngestionStatus()
        );

        verify(vectorStore, never()).add(anyList());
        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    void storesVectorsAndChunksAndMarksDocumentCompleted() {

        com.documind.api.entity.Document document = createDocument();

        ingestionService.ingest(
                document,
                "DocuMind stores documents for semantic search."
        );

        ArgumentCaptor<List<Document>> vectorCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(vectorStore).add(vectorCaptor.capture());

        List<Document> vectorDocuments = vectorCaptor.getValue();

        assertEquals(1, vectorDocuments.size());

        assertEquals(
                "DocuMind stores documents for semantic search.",
                vectorDocuments.get(0).getText()
        );

        assertEquals(
                document.getId().toString(),
                vectorDocuments.get(0).getMetadata().get("documentId")
        );

        assertEquals(
                document.getOwner().getId().toString(),
                vectorDocuments.get(0).getMetadata().get("userId")
        );

        assertEquals(
                document.getFileName(),
                vectorDocuments.get(0).getMetadata().get("fileName")
        );

        assertEquals(
                0,
                vectorDocuments.get(0).getMetadata().get("chunkIndex")
        );

        ArgumentCaptor<List<DocumentChunk>> chunkCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(chunkRepository).saveAll(chunkCaptor.capture());

        List<DocumentChunk> savedChunks = chunkCaptor.getValue();

        assertEquals(1, savedChunks.size());

        assertEquals(
                "DocuMind stores documents for semantic search.",
                savedChunks.get(0).getChunkText()
        );

        assertEquals(0, savedChunks.get(0).getChunkIndex());

        assertEquals(
                IngestionStatus.COMPLETED,
                document.getIngestionStatus()
        );

        verify(documentRepository, org.mockito.Mockito.times(2))
                .save(document);
    }

    @Test
    void cleansUpAndMarksDocumentFailedWhenVectorInsertionFails() {

        com.documind.api.entity.Document document = createDocument();

        doThrow(new RuntimeException("Vector insertion failed"))
                .when(vectorStore)
                .add(anyList());

        assertThrows(
                RuntimeException.class,
                () -> ingestionService.ingest(
                        document,
                        "DocuMind stores documents for semantic search."
                )
        );

        assertEquals(
                IngestionStatus.FAILED,
                document.getIngestionStatus()
        );

        // One deletion before ingestion and another during failure cleanup.
        verify(vectorStore, org.mockito.Mockito.times(2))
                .delete(
                        new FilterExpressionBuilder()
                                .eq("documentId", document.getId().toString())
                                .build()
                );

        verify(chunkRepository, org.mockito.Mockito.times(2))
                .deleteByDocument_Id(document.getId());

        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    void deletesVectorsAndChunkRecordsForDocument() {

        com.documind.api.entity.Document document = createDocument();

        ingestionService.deleteVectorsForDocument(document);

        verify(vectorStore).delete(
                new FilterExpressionBuilder()
                        .eq("documentId", document.getId().toString())
                        .build()
        );

        verify(chunkRepository)
                .deleteByDocument_Id(document.getId());
    }

    private com.documind.api.entity.Document createDocument() {

        com.documind.api.entity.Document document =
                new com.documind.api.entity.Document(
                        "sample.txt",
                        48L,
                        "text/plain",
                        "sample-storage.txt"
                );

        try {
            var idField = document.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(document, UUID.randomUUID());

        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to assign test document ID",
                    exception
            );
        }

        // Every ingested document must have an owner.
        User owner = new User();

        try {
            var userIdField = User.class.getDeclaredField("id");
            userIdField.setAccessible(true);
            userIdField.set(owner, UUID.randomUUID());

        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to assign test user ID",
                    exception
            );
        }

        document.setOwner(owner);

        return document;
    }
}
