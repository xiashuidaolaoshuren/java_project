package com.focusflow;

import com.focusflow.ai.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenAiProperties.class)
public class FocusFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(FocusFlowApplication.class, args);
	}
}
