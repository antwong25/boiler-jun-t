package org.example.boilerserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.azure")
public class AzureOpenAiProperties {
    private String endpoint;
    private String apiKey;
    private String apiVersion = "2025-04-01-preview";
    private String model = "gpt-small";
}
