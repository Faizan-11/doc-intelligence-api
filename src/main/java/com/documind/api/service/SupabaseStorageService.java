
package com.documind.api.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageService {

    private final String projectUrl;
    private final String secretKey;
    private final String bucket;
    private final HttpClient httpClient;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String projectUrl,
            @Value("${supabase.secret.key:}") String secretKey,
            @Value("${supabase.bucket:documind-documents}") String bucket
    ) {
        this.projectUrl = projectUrl;
        this.secretKey = secretKey;
        this.bucket = bucket;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public void upload(
            String objectName,
            byte[] content,
            String contentType
    ) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(storageUri(objectName))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        send(request, "upload");
    }

    public byte[] download(String objectName) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(storageUri(objectName))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Supabase download failed with HTTP "
                                + response.statusCode()
                );
            }

            return response.body();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase download interrupted.", exception);
        }
    }

    public void delete(String objectName) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(storageUri(objectName))
                .timeout(Duration.ofSeconds(60))
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .DELETE()
                .build();

        send(request, "delete");
    }

    private URI storageUri(String objectName) {
        String encodedObjectName = java.util.Arrays.stream(
                objectName.split("/", -1)
        )
        .map(part -> URLEncoder.encode(
                part,
                StandardCharsets.UTF_8
        ).replace("+", "%20"))
        .collect(java.util.stream.Collectors.joining("/"));

        return URI.create(
                projectUrl.replaceAll("/+$", "")
                        + "/storage/v1/object/"
                        + bucket
                        + "/"
                        + encodedObjectName
        );
    }

    private void send(
            HttpRequest request,
            String operation
    ) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IOException(
                        "Supabase " + operation
                                + " failed with HTTP "
                                + response.statusCode()
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase " + operation + " interrupted.",
                    exception
            );
        }
    }
}
