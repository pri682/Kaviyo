package com.kaviyo.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BasetenMuseSparkOpenClient {
    public static final String PROVIDER = "BASETEN";
    public static final String MODEL_ID = "meta-models/muse-spark-open-1026";
    private static final String CHAT_COMPLETIONS_URL = "https://inference.baseten.co/v1/chat/completions";

    @ConfigProperty(name = "BASETEN_API_KEY", defaultValue = "")
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
