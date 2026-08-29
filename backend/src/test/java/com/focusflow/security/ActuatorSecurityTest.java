package com.focusflow.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focusflow.testsupport.DailyPlanAiTestConfiguration;
import com.focusflow.testsupport.PostgresTestcontainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({DailyPlanAiTestConfiguration.class, PostgresTestcontainerConfig.class})
class ActuatorSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void liveness_isPublicAndDetailFree() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.details").doesNotExist())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void readiness_isPublicAndDetailFree() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.details").doesNotExist())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void aggregateHealth_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
	}

	@Test
	void metrics_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
	}
}
