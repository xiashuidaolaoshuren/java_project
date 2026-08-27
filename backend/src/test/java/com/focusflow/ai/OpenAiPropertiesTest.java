package com.focusflow.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@EnableConfigurationProperties(OpenAiProperties.class)
class OpenAiPropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(OpenAiPropertiesTestConfiguration.class);

	@Test
	void bindsApiKeyAndModelFromConfiguration() {
		contextRunner
				.withPropertyValues(
						"focusflow.openai.api-key=test-api-key",
						"focusflow.openai.model=gpt-4.1-mini")
				.run(
						context -> {
							OpenAiProperties properties = context.getBean(OpenAiProperties.class);
							assertThat(properties.apiKey()).isEqualTo("test-api-key");
							assertThat(properties.model()).isEqualTo("gpt-4.1-mini");
							assertThat(properties.baseUrl()).isEqualTo("https://api.openai.com/v1");
							assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
							assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
							assertThat(properties.maxAttempts()).isEqualTo(2);
							assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(1));
							assertThat(properties.maxRetryAfter()).isEqualTo(Duration.ofSeconds(5));
						});
	}

	@Test
	void bindsCustomBaseUrlFromConfiguration() {
		contextRunner
				.withPropertyValues(
						"focusflow.openai.api-key=test-api-key",
						"focusflow.openai.model=deepseek-chat",
						"focusflow.openai.base-url=https://api.deepseek.com/v1")
				.run(
						context -> {
							OpenAiProperties properties = context.getBean(OpenAiProperties.class);
							assertThat(properties.baseUrl()).isEqualTo("https://api.deepseek.com/v1");
							assertThat(properties.model()).isEqualTo("deepseek-chat");
						});
	}

	@Test
	void bindsTimeoutAndRetryOverridesFromConfiguration() {
		contextRunner
				.withPropertyValues(
						"focusflow.openai.api-key=test-api-key",
						"focusflow.openai.model=gpt-4.1-mini",
						"focusflow.openai.connect-timeout=3s",
						"focusflow.openai.read-timeout=45s",
						"focusflow.openai.max-attempts=3",
						"focusflow.openai.retry-delay=2s",
						"focusflow.openai.max-retry-after=10s")
				.run(
						context -> {
							OpenAiProperties properties = context.getBean(OpenAiProperties.class);
							assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
							assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(45));
							assertThat(properties.maxAttempts()).isEqualTo(3);
							assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(2));
							assertThat(properties.maxRetryAfter()).isEqualTo(Duration.ofSeconds(10));
						});
	}

	@org.springframework.context.annotation.Configuration
	@EnableConfigurationProperties(OpenAiProperties.class)
	static class OpenAiPropertiesTestConfiguration {}
}
