
package com.documind.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import com.documind.api.entity.DocumentChunk;
import com.documind.api.repository.DocumentChunkRepository;



@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final TextChunkingService textChunkingService;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentIngestionService(
            VectorStore vectorStore,
            TextChunkingService textChunkingService,
            DocumentChunkRepository documentChunkRepository
    ) {
        this.vectorStore = vectorStore;
        this.textChunkingService = textChunkingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    public void ingest(
            com.documind.api.entity.Document document,
            String extractedText
    ) {
        if (extractedText == null || extractedText.isBlank()) {
            return;
        }

        List<String> chunks =
                textChunkingService.splitIntoChunks(extractedText);

        if (chunks.isEmpty()) {
            return;
        }

        List<org.springframework.ai.document.Document> vectorDocuments =
                new ArrayList<>();

        List<DocumentChunk> databaseChunks = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            String chunkText = chunks.get(index);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", document.getId().toString());
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

        vectorStore.add(vectorDocuments);
        documentChunkRepository.saveAll(databaseChunks);
    }

    public void deleteVectorsForDocument(
        com.documind.api.entity.Document document
) {
    FilterExpressionBuilder filterBuilder =
            new FilterExpressionBuilder();

    vectorStore.delete(
            filterBuilder
                    .eq("documentId", document.getId().toString())
                    .build()
    );

    documentChunkRepository.deleteByDocument_Id(document.getId());
}
}
