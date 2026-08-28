package com.focusflow.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focusflow.testsupport.DailyPlanAiTestConfiguration;
import com.focusflow.testsupport.PostgresTestcontainerConfig;
import org.junit.jupiter.api.Nested;
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
class SpringdocExposureTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocs_isAbsentWithoutDevProfile() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
	}

	@Test
	void swaggerUi_isAbsentWithoutDevProfile() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
	}

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@ActiveProfiles({"test", "dev"})
	@Import({DailyPlanAiTestConfiguration.class, PostgresTestcontainerConfig.class})
	class DevProfile {

		@Autowired
		private MockMvc devMockMvc;

		@Test
		void apiDocs_isAvailableUnderDevProfile() throws Exception {
			devMockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		}

		@Test
		void swaggerUi_isAvailableUnderDevProfile() throws Exception {
			devMockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		}
	}
}
