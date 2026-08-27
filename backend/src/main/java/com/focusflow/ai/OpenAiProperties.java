package com.focusflow.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "focusflow.openai")
public record OpenAiProperties(
		String apiKey,
		String model,
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		int maxAttempts,
		Duration retryDelay,
		Duration maxRetryAfter) {

	public OpenAiProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.openai.com/v1";
		}
		if (connectTimeout == null) {
			connectTimeout = Duration.ofSeconds(2);
		}
		if (readTimeout == null) {
			readTimeout = Duration.ofSeconds(30);
		}
		if (maxAttempts <= 0) {
			maxAttempts = 2;
		}
		if (retryDelay == null) {
			retryDelay = Duration.ofSeconds(1);
		}
		if (maxRetryAfter == null) {
			maxRetryAfter = Duration.ofSeconds(5);
		}
	}
}
