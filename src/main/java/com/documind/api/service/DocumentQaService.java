
package com.documind.api.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
public class DocumentQaService {

    private final ChatClient chatClient;
    private final DocumentRetrievalService retrievalService;

    public DocumentQaService(
            ChatClient.Builder chatClientBuilder,
            DocumentRetrievalService retrievalService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.retrievalService = retrievalService;
    }

    public QaResponse ask(String question, int topK) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question must not be blank."
            );
        }

        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException(
                    "topK must be between 1 and 20."
            );
        }

        List<Document> retrievedDocuments =
                retrievalService.search(question, topK);

        if (retrievedDocuments.isEmpty()) {
            return new QaResponse(
                    "I couldn't find relevant information in the uploaded documents to answer this question.",
                    List.of()
            );
        }

        String context = retrievedDocuments.stream()
                .map(document -> {
                    Object fileName =
                            document.getMetadata().get("fileName");

                    String source = fileName == null
                            ? "Unknown source"
                            : fileName.toString();

                    return "Source: " + source
                            + "\nContent:\n" + document.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = chatClient.prompt()
                .system("""
                        You are DocuMind, a document question-answering
                        assistant.

                        Answer the user's question using only the
                        supplied document context.

                        Do not invent facts or rely on outside knowledge
                        to fill gaps. If the context does not contain
                        enough information, clearly say so.

                        Be clear, accurate, and concise. When relevant,
                        mention the source filename supporting the answer.
                        """)
                .user(user -> user.text("""
                        Question:
                        {question}

                        Retrieved document context:
                        {context}
                        """)
                        .param("question", question.trim())
                        .param("context", context))
                .call()
                .content();

        Set<String> uniqueSources = new LinkedHashSet<>();

        for (Document document : retrievedDocuments) {
            Object fileName = document.getMetadata().get("fileName");

            if (fileName != null) {
                uniqueSources.add(fileName.toString());
            }
        }

        return new QaResponse(
                answer == null || answer.isBlank()
                        ? "I couldn't generate an answer from the retrieved context."
                        : answer.trim(),
                new ArrayList<>(uniqueSources)
        );
    }

    public record QaResponse(
            String answer,
            List<String> sources
    ) {
    }
}
