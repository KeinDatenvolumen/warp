package de.warp.weather.infrastructure.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "warp.weather.gemini")
public record GeminiProperties(
        URI baseUrl,
        String apiKey,
        String model,
        String fallbackModel,
        Duration connectTimeout,
        Duration readTimeout,
        Double temperature
) {

    public GeminiProperties {
        if (baseUrl == null) {
            baseUrl = URI.create("https://generativelanguage.googleapis.com");
        }
        if (apiKey == null) {
            apiKey = "";
        }
        if (model == null || model.isBlank() || "gemini-2.5-flash".equals(model) || "gemini-2.0-flash".equals(model)) {
            model = "gemini-3.5-flash";
        }
        if (fallbackModel == null || fallbackModel.isBlank() || "gemini-2.0-flash".equals(fallbackModel) || "gemini-2.5-flash".equals(fallbackModel)) {
            fallbackModel = "gemini-3.6-flash";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(20);
        }
        if (temperature == null) {
            temperature = 0.2;
        }
    }

    public static GeminiProperties defaults() {
        return new GeminiProperties(null, null, null, null, null, null, null);
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your_");
    }
}
