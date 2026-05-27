package com.outbound.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Downloads and caches the Genesys Cloud Swagger JSON — once per test run.
 * The Swagger file is ~20MB. If every test class downloaded it on its own,
 * we'd be waiting 60+ extra seconds every run — just for the same file.
 * This class grabs it once, keeps it in memory, and hands it out to whoever
 * needs it (SwaggerApiTest, and any future test classes).
 */
public class SwaggerSpecCache {

    public static final String SWAGGER_URL = "https://api.mypurecloud.com/api/v2/docs/swagger";

    private static final AtomicReference<JsonNode> specRoot = new AtomicReference<>();
    private static final AtomicReference<String> specRawBody = new AtomicReference<>();
    private static final AtomicReference<Integer> httpStatus = new AtomicReference<>(0);

    private SwaggerSpecCache() {}

    public static JsonNode getRoot() {
        ensureLoaded();
        return specRoot.get();
    }

    public static int getHttpStatus() {
        ensureLoaded();
        return httpStatus.get();
    }

    private static void ensureLoaded() {
        if (specRoot.get() == null) {
            synchronized (SwaggerSpecCache.class) {
                if (specRoot.get() == null) {
                    fetch();
                }
            }
        }
    }

    private static void fetch() {
        try {
            System.out.println("[SwaggerSpecCache] Downloading spec (one-time, ~20MB)...");
            long start = System.currentTimeMillis();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SWAGGER_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            httpStatus.set(response.statusCode());
            specRawBody.set(response.body());

            if (httpStatus.get() == 200) {
                specRoot.set(new ObjectMapper().readTree(specRawBody.get()));
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("[SwaggerSpecCache] Downloaded in %dms (status=%d)%n", elapsed, httpStatus.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching Swagger spec from " + SWAGGER_URL, e);
        } catch (Exception e) {
            System.err.println("[SwaggerSpecCache] FETCH FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("[SwaggerSpecCache] Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to fetch Swagger spec from " + SWAGGER_URL, e);
        }
    }
}
