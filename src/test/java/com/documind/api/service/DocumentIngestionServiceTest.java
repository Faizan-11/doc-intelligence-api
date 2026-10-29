
package com.documind.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import com.documind.api.entity.DocumentChunk;
import com.documind.api.repository.DocumentChunkRepository;

class DocumentIngestionServiceTest {

    private VectorStore vectorStore;
    private TextChunkingService textChunkingService;
    private DocumentChunkRepository chunkRepository;
    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        textChunkingService = new TextChunkingService();
        chunkRepository = mock(DocumentChunkRepository.class);

        ingestionService = new DocumentIngestionService(
                vectorStore,
                textChunkingService,
                chunkRepository
        );
    }

    @Test
    void ignoresNullOrBlankText() {
        com.documind.api.entity.Document document = createDocument();

        ingestionService.ingest(document, null);
        ingestionService.ingest(document, "   ");

        verify(vectorStore, never()).add(anyList());
        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    void storesVectorDocumentsAndMatchingChunkRecords() {
        com.documind.api.entity.Document document = createDocument();

        ingestionService.ingest(
                document,
                "DocuMind stores documents for semantic search."
        );

        org.mockito.ArgumentCaptor<List<Document>> vectorCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);

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
                document.getFileName(),
                vectorDocuments.get(0).getMetadata().get("fileName")
        );
        assertEquals(
                0,
                vectorDocuments.get(0).getMetadata().get("chunkIndex")
        );

        org.mockito.ArgumentCaptor<List<DocumentChunk>> chunkCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);

        verify(chunkRepository).saveAll(chunkCaptor.capture());

        List<DocumentChunk> savedChunks = chunkCaptor.getValue();
        assertEquals(1, savedChunks.size());
        assertEquals(
                "DocuMind stores documents for semantic search.",
                savedChunks.get(0).getChunkText()
        );
        assertEquals(0, savedChunks.get(0).getChunkIndex());
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

        verify(chunkRepository).deleteByDocument_Id(document.getId());
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

        return document;
    }
}
