package com.focusflow.ai;

import static org.assertj.core.api.Assertions.assertThat;

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

	@org.springframework.context.annotation.Configuration
	@EnableConfigurationProperties(OpenAiProperties.class)
	static class OpenAiPropertiesTestConfiguration {}
}
