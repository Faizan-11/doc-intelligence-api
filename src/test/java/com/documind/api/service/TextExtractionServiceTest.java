
package com.documind.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

class TextExtractionServiceTest {

    @TempDir
    Path tempDir;

    private final TextExtractionService service =
            new TextExtractionService();

    @Test
    void extractsTextFromTxtFile() throws IOException {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "Hello DocuMind!", StandardCharsets.UTF_8);

        String result = service.extractText(file, "sample.txt");

        assertEquals("Hello DocuMind!", result);
    }

    @Test
    void extractsTextFromMarkdownFile() throws IOException {
        Path file = tempDir.resolve("sample.md");
        Files.writeString(file, "# DocuMind\nText extraction works.");

        String result = service.extractText(file, "sample.md");

        assertEquals("# DocuMind\nText extraction works.", result);
    }

    @Test
    void rejectsUnsupportedFileType() {
        Path file = tempDir.resolve("sample.docx");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.extractText(file, "sample.docx")
        );
    }


@Test
void extractsTextFromPdfFile() throws IOException {
    Path file = tempDir.resolve("sample.pdf");

    try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    12
            );
            content.newLineAtOffset(50, 700);
            content.showText("Hello DocuMind PDF!");
            content.endText();
        }

        document.save(file.toFile());
    }

    String result = service.extractText(file, "sample.pdf");

    org.junit.jupiter.api.Assertions.assertTrue(
            result.contains("Hello DocuMind PDF!")
    );
}

}
