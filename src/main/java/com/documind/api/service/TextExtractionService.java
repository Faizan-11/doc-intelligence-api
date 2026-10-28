
package com.documind.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

    public String extractText(Path filePath, String originalFileName)
            throws IOException {

        String fileName = originalFileName.toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }

        if (fileName.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
                PDFTextStripper textStripper = new PDFTextStripper();
                return textStripper.getText(document);
            }
        }

        throw new IllegalArgumentException(
                "Text extraction is not supported for this file type yet."
        );
    }
}
