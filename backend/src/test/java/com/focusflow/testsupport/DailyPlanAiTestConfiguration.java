package com.focusflow.testsupport;

import com.focusflow.ai.AiDailyPlanResponse;
import com.focusflow.ai.DailyPlanAiClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DailyPlanAiTestConfiguration {

	@Bean
	DailyPlanAiClient dailyPlanAiClient() {
		return request -> new AiDailyPlanResponse(java.util.List.of());
	}
}
