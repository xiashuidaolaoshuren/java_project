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
						});
	}

	@org.springframework.context.annotation.Configuration
	@EnableConfigurationProperties(OpenAiProperties.class)
	static class OpenAiPropertiesTestConfiguration {}
}
