
package com.documind.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextChunkingServiceTest {

    private final TextChunkingService service = new TextChunkingService();

    @Test
    void returnsNoChunksForNullOrBlankText() {
        assertTrue(service.splitIntoChunks(null).isEmpty());
        assertTrue(service.splitIntoChunks("").isEmpty());
        assertTrue(service.splitIntoChunks("   \n  ").isEmpty());
    }

    @Test
    void keepsShortTextInOneChunk() {
        List<String> chunks = service.splitIntoChunks("Hello DocuMind!");

        assertEquals(1, chunks.size());
        assertEquals("Hello DocuMind!", chunks.get(0));
    }

    @Test
    void splitsLongTextIntoMultipleChunks() {
        String text = "word ".repeat(500);

        List<String> chunks = service.splitIntoChunks(text);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> !chunk.isBlank()));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 1000));
    }

    @Test
    void preservesOverlapBetweenAdjacentChunks() {
        String text = "abcdefghij".repeat(250);

        List<String> chunks = service.splitIntoChunks(text);

        assertTrue(chunks.size() > 1);

        String firstChunk = chunks.get(0);
        String secondChunk = chunks.get(1);

        assertTrue(firstChunk.length() > 150);
        assertTrue(secondChunk.length() > 0);

        // The second chunk should begin with content from the end of the first.
        assertTrue(
                firstChunk.endsWith(secondChunk.substring(0, 150)),
                "Adjacent chunks should share the configured overlap"
        );
    }

    @Test
    void stripsLeadingAndTrailingWhitespace() {
        List<String> chunks = service.splitIntoChunks("  Hello world  ");

        assertEquals(1, chunks.size());
        assertEquals("Hello world", chunks.get(0));
        assertFalse(chunks.get(0).startsWith(" "));
        assertFalse(chunks.get(0).endsWith(" "));
    }
}
