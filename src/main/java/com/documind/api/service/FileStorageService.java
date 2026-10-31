package com.documind.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final SupabaseStorageService supabaseStorageService;

    public FileStorageService(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    public String store(MultipartFile file) throws IOException {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? "document"
                        : file.getOriginalFilename()
        );

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');

        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        String storageFileName = UUID.randomUUID() + extension;

        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();

        supabaseStorageService.upload(
                storageFileName,
                file.getBytes(),
                contentType
        );

        return storageFileName;
    }

    /**
     * Downloads a temporary copy for text extraction only.
     */
    public Path resolve(String storageFileName) throws IOException {
        validateStorageFileName(storageFileName);

        byte[] content = supabaseStorageService.download(storageFileName);

        String suffix = storageFileName.contains(".")
                ? storageFileName.substring(storageFileName.lastIndexOf('.'))
                : ".tmp";

        Path temporaryFile = Files.createTempFile("documind-", suffix);

        try {
            Files.write(temporaryFile, content);
            return temporaryFile;
        } catch (IOException exception) {
            Files.deleteIfExists(temporaryFile);
            throw exception;
        }
    }

    /**
     * Removes the temporary extraction copy.
     */
    public void release(Path temporaryFile) throws IOException {
        if (temporaryFile != null) {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Reads the permanent file directly from Supabase.
     */
    public byte[] read(String storageFileName) throws IOException {
        validateStorageFileName(storageFileName);
        return supabaseStorageService.download(storageFileName);
    }

    /**
     * Deletes the permanent file from Supabase.
     */
    public void delete(String storageFileName) throws IOException {
        validateStorageFileName(storageFileName);
        supabaseStorageService.delete(storageFileName);
    }

    private void validateStorageFileName(String storageFileName) {
        if (storageFileName == null
                || !storageFileName.matches(
                        "[a-fA-F0-9-]{36}(\\.[a-zA-Z0-9]+)?"
                )) {
            throw new IllegalArgumentException("Invalid storage filename.");
        }
    }
}