package com.focusflow;

import com.focusflow.testsupport.DailyPlanAiTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(DailyPlanAiTestConfiguration.class)
class FocusFlowApplicationTests {

	@Test
	void contextLoads() {
	}
}
