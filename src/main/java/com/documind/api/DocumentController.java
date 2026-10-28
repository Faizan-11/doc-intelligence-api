
package com.documind.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.documind.api.entity.Document;
import com.documind.api.repository.DocumentRepository;
import com.documind.api.service.FileStorageService;
import com.documind.api.service.TextExtractionService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of(".pdf", ".txt", ".md", ".doc", ".docx");

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;

    public DocumentController(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            TextExtractionService textExtractionService
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
    }

    @GetMapping
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }


@GetMapping("/search")
public List<Document> searchDocuments(
        @org.springframework.web.bind.annotation.RequestParam String keyword
) {
    if (keyword == null || keyword.isBlank()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Search keyword must not be blank."
        );
    }

    return documentRepository.findByExtractedTextContainingIgnoreCase(
            keyword.trim()
    );
}



@GetMapping("/{id}/text")
public ResponseEntity<String> getExtractedText(@PathVariable UUID id) {
    Document document = findDocument(id);

    String extractedText = document.getExtractedText();

    if (extractedText == null) {
        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Extracted text is not available for this document."
        );
    }

    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(extractedText);
}


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Document uploadDocument(
            @RequestPart("file") MultipartFile file
    ) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String safeFileName = Path.of(originalName).getFileName().toString();

        String storageFileName;

        try {
            storageFileName = fileStorageService.store(file);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store uploaded file.",
                    exception
            );
        }

        try {
            Document document = new Document(
                    safeFileName,
                    file.getSize(),
                    file.getContentType(),
                    storageFileName
            );

            String lowerName = safeFileName.toLowerCase(Locale.ROOT);

            if (lowerName.endsWith(".pdf")
                    || lowerName.endsWith(".txt")
                    || lowerName.endsWith(".md")) {

                Path storedPath = fileStorageService.resolve(storageFileName);

                String extractedText = textExtractionService.extractText(
                        storedPath,
                        safeFileName
                );

                document.setExtractedText(extractedText);
            }

            return documentRepository.save(document);

        } catch (IOException | RuntimeException exception) {
            try {
                fileStorageService.delete(storageFileName);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            if (exception instanceof ResponseStatusException responseException) {
                throw responseException;
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to process the uploaded document.",
                    exception
            );
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id
    ) {
        Document document = findDocument(id);

        try {
            Path path = fileStorageService.resolve(
                    document.getStorageFileName()
            );

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Stored file not found."
                );
            }

            String contentType = document.getContentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                    : document.getContentType();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    document.getFileName().replace("\"", "") +
                                    "\""
                    )
                    .body(resource);

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read stored file.",
                    exception
            );
        }
    }

    @DeleteMapping("/{id}")
    public void deleteDocument(@PathVariable UUID id) {
        Document document = findDocument(id);

        documentRepository.delete(document);
        documentRepository.flush();

        try {
            fileStorageService.delete(document.getStorageFileName());
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The database record was deleted, but the stored file could not be removed.",
                    exception
            );
        }
    }

    private Document findDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file is empty."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Maximum file size is 10 MiB."
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A filename is required."
            );
        }

        String lowerName = originalName.toLowerCase(Locale.ROOT);
        int dotIndex = lowerName.lastIndexOf('.');
        String extension = dotIndex >= 0
                ? lowerName.substring(dotIndex)
                : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: PDF, TXT, MD, DOC, DOCX."
            );
        }
    }
}
