package com.kaviyo.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MetaSparkClient {
    public static final String PROVIDER = "META_MODEL_API";
    public static final String MODEL_ID = "muse-spark-1.3";
    private static final String CHAT_COMPLETIONS_URL = "https://api.meta.ai/v1/chat/completions";

    @ConfigProperty(name = "MODEL_API_KEY", defaultValue = "")
    String apiKey;

    @Inject
    OpenAiCompatibleChatClient chatClient;

    public JsonNode complete(ObjectNode request) {
        request.put("model", MODEL_ID);
        return chatClient.complete(CHAT_COMPLETIONS_URL, apiKey, request);
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
