package com.focusflow.testsupport;

import com.focusflow.ai.AiDailyPlanResponse;
import com.focusflow.ai.DailyPlanAiClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class DailyPlanAiTestConfiguration {

	@Bean
	@Primary
	DailyPlanAiClient dailyPlanAiClient() {
		return request -> new AiDailyPlanResponse(java.util.List.of());
	}
}
