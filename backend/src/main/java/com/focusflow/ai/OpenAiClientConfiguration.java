package com.focusflow.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfiguration {

	@Bean
	RestClient openAiRestClient(OpenAiProperties properties) {
		return RestClient.builder().baseUrl(properties.baseUrl()).build();
	}

	@Bean
	@ConditionalOnMissingBean(DailyPlanAiClient.class)
	OpenAiDailyPlanClient openAiDailyPlanClient(
			RestClient openAiRestClient,
			DailyPlanPromptBuilder promptBuilder,
			OpenAiProperties properties,
			com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
		return new OpenAiDailyPlanClient(openAiRestClient, promptBuilder, properties, objectMapper);
	}
}
