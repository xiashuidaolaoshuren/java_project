package com.focusflow.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "focusflow.openai")
public record OpenAiProperties(String apiKey, String model, String baseUrl) {

	public OpenAiProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.openai.com/v1";
		}
	}
}
