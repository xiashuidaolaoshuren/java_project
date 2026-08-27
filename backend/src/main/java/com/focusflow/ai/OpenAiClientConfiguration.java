package com.focusflow.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfiguration {

	@Bean
	Sleeper sleeper() {
		return duration -> Thread.sleep(duration.toMillis());
	}

	@Bean
	RestClient openAiRestClient(OpenAiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(DailyPlanAiClient.class)
	OpenAiDailyPlanClient openAiDailyPlanClient(
			RestClient openAiRestClient,
			DailyPlanPromptBuilder promptBuilder,
			OpenAiProperties properties,
			com.fasterxml.jackson.databind.ObjectMapper objectMapper,
			Sleeper sleeper) {
		return new OpenAiDailyPlanClient(
				openAiRestClient, promptBuilder, properties, objectMapper, sleeper);
	}
}
