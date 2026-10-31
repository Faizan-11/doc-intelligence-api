package com.documind.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import com.documind.api.entity.DocumentChunk;
import com.documind.api.entity.IngestionStatus;
import com.documind.api.repository.DocumentChunkRepository;
import com.documind.api.repository.DocumentRepository;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final TextChunkingService textChunkingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public DocumentIngestionService(
            VectorStore vectorStore,
            TextChunkingService textChunkingService,
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository
    ) {
        this.vectorStore = vectorStore;
        this.textChunkingService = textChunkingService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    public void ingest(
            com.documind.api.entity.Document document,
            String extractedText
    ) {
        if (extractedText == null || extractedText.isBlank()) {
            document.setIngestionStatus(IngestionStatus.FAILED);
            documentRepository.save(document);

            throw new IllegalArgumentException(
                    "Cannot ingest a document with empty extracted text."
            );
        }

        // Persist the start of the ingestion process.
        document.setIngestionStatus(IngestionStatus.PROCESSING);
        documentRepository.save(document);

        try {
            // Remove any previous data before re-ingesting.
            deleteVectorsForDocument(document);

            List<String> chunks =
                    textChunkingService.splitIntoChunks(extractedText);

            if (chunks.isEmpty()) {
                throw new IllegalStateException(
                        "No text chunks were generated for this document."
                );
            }

            List<org.springframework.ai.document.Document> vectorDocuments =
                    new ArrayList<>();

            List<DocumentChunk> databaseChunks = new ArrayList<>();

            for (int index = 0; index < chunks.size(); index++) {
                String chunkText = chunks.get(index);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", document.getId().toString());
                metadata.put("userId", document.getOwner().getId().toString());
                metadata.put("fileName", document.getFileName());
                metadata.put("chunkIndex", index);

                vectorDocuments.add(
                        org.springframework.ai.document.Document.builder()
                                .text(chunkText)
                                .metadata(metadata)
                                .build()
                );

                databaseChunks.add(
                        new DocumentChunk(document, index, chunkText)
                );
            }

            // Store embeddings and corresponding database chunks.
            vectorStore.add(vectorDocuments);
            documentChunkRepository.saveAll(databaseChunks);

            // Mark ingestion successful only after both operations succeed.
            document.setIngestionStatus(IngestionStatus.COMPLETED);
            documentRepository.save(document);

        } catch (RuntimeException exception) {

            // Best-effort cleanup to avoid leaving partial ingestion data.
            try {
                deleteVectorsForDocument(document);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            document.setIngestionStatus(IngestionStatus.FAILED);

            try {
                documentRepository.save(document);
            } catch (RuntimeException statusException) {
                exception.addSuppressed(statusException);
            }

            throw exception;
        }
    }

    public void deleteVectorsForDocument(
            com.documind.api.entity.Document document
    ) {
        FilterExpressionBuilder filterBuilder =
                new FilterExpressionBuilder();

        RuntimeException cleanupFailure = null;

        // Attempt vector cleanup.
        try {
            vectorStore.delete(
                    filterBuilder
                            .eq("documentId", document.getId().toString())
                            .build()
            );
        } catch (RuntimeException exception) {
            cleanupFailure = exception;
        }

        // Attempt chunk cleanup even if vector cleanup failed.
        try {
            documentChunkRepository.deleteByDocument_Id(document.getId());
        } catch (RuntimeException exception) {
            if (cleanupFailure == null) {
                cleanupFailure = exception;
            } else {
                cleanupFailure.addSuppressed(exception);
            }
        }

        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }
}