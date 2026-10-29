package com.kaviyo.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal OpenAI-compatible transport; callers pin endpoint and Meta model IDs. */
@ApplicationScoped
public class OpenAiCompatibleChatClient {
    private static final int MAX_REQUEST_BYTES = 256 * 1024;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Inject
    ObjectMapper objectMapper;

    public JsonNode complete(String endpoint, String apiKey, ObjectNode request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ModelProviderException("PROVIDER_NOT_CONFIGURED",
                    "The model provider key is not configured.", 503, null);
        }
        try {
            byte[] body = objectMapper.writeValueAsBytes(request);
            if (body.length > MAX_REQUEST_BYTES) {
                throw new ModelProviderException("MODEL_REQUEST_TOO_LARGE",
                        "The model request exceeds the configured size limit.", 413, null);
            }
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(75))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            byte[] responseBody;
            try (InputStream stream = response.body()) {
                responseBody = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
            }
            if (responseBody.length > MAX_RESPONSE_BYTES) {
                throw new ModelProviderException("MODEL_RESPONSE_TOO_LARGE",
                        "The model provider response exceeds the configured size limit.", 502, null);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(response.statusCode());
            }
            return objectMapper.readTree(responseBody);
        } catch (ModelProviderException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModelProviderException("MODEL_CALL_INTERRUPTED",
                    "The model call was interrupted.", 503, exception);
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new ModelProviderException("MODEL_CALL_TIMEOUT",
                    "The model provider did not respond before the timeout.", 504, exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ModelProviderException("MODEL_PROVIDER_UNAVAILABLE",
                    "The model provider could not be reached or returned invalid JSON.", 502, exception);
        }
    }

    private static ModelProviderException providerError(int status) {
        if (status == 401 || status == 403) {
            return new ModelProviderException("MODEL_AUTHENTICATION_FAILED",
                    "The configured model key was rejected.", 502, null);
        }
        if (status == 429) {
            return new ModelProviderException("MODEL_RATE_LIMITED",
                    "The model provider rate limit was reached.", 503, null);
        }
        if (status >= 500) {
            return new ModelProviderException("MODEL_PROVIDER_ERROR",
                    "The model provider returned a temporary error.", 502, null);
        }
        return new ModelProviderException("MODEL_REQUEST_REJECTED",
                "The model provider rejected the request. Check the request schema and model ID.", 502, null);
    }
}
