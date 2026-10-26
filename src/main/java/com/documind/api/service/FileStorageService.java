
package com.documind.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path storageDirectory;

    public FileStorageService(
            @Value("${documind.storage.directory:}") String configuredDirectory
    ) throws IOException {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException(
                    "DocuMind storage directory must be configured."
            );
        }

        this.storageDirectory = Path.of(configuredDirectory)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(this.storageDirectory);
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
        Path destination = storageDirectory.resolve(storageFileName)
                .normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IOException("Invalid storage path.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        return storageFileName;
    }

    public Path resolve(String storageFileName) {
        Path resolvedPath = storageDirectory.resolve(storageFileName)
                .normalize();

        if (!resolvedPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage filename.");
        }

        return resolvedPath;
    }

    public void delete(String storageFileName) throws IOException {
        Files.deleteIfExists(resolve(storageFileName));
    }
}
