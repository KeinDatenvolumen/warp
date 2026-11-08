// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
            connectTimeout = Duration.ofSeconds(2);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(8);
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
