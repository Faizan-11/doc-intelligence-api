
package com.documind.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TextChunkingService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 150;

    public List<String> splitIntoChunks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalizedText = text.strip();

        if (normalizedText.length() <= CHUNK_SIZE) {
            return List.of(normalizedText);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalizedText.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalizedText.length());

            if (end < normalizedText.length()) {
                int paragraphBreak = normalizedText.lastIndexOf("\n\n", end);
                int sentenceBreak = normalizedText.lastIndexOf(". ", end);
                int wordBreak = normalizedText.lastIndexOf(' ', end);

                int preferredBreak = Math.max(
                        paragraphBreak >= start ? paragraphBreak : -1,
                        Math.max(
                                sentenceBreak >= start ? sentenceBreak + 1 : -1,
                                wordBreak >= start ? wordBreak : -1
                        )
                );

                if (preferredBreak > start + CHUNK_SIZE / 2) {
                    end = preferredBreak;
                }
            }

            String chunk = normalizedText.substring(start, end).strip();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= normalizedText.length()) {
                break;
            }

            int nextStart = end - CHUNK_OVERLAP;

            // Always move forward, even if a preferred break is unusually close.
            start = Math.max(start + 1, nextStart);
        }

        return List.copyOf(chunks);
    }
}
