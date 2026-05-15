package com.focusflow.common.error;

import com.focusflow.ai.AiProviderException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/__test/errors")
public class GlobalExceptionHandlerTestController {

	@GetMapping("/not-found")
	void notFound() {
		throw new NotFoundException("missing");
	}

	@GetMapping("/forbidden")
	void forbidden() {
		throw new ForbiddenOperationException("nope");
	}

	@GetMapping("/ai-provider")
	void aiProvider() {
		throw new AiProviderException("provider failed");
	}

	@GetMapping("/generic")
	void generic() {
		throw new RuntimeException("boom");
	}

	@PostMapping("/validation")
	void validation(@Valid @RequestBody ValidationDto body) {
		throw new IllegalStateException("should not run: " + body);
	}

	static class ValidationDto {
		@NotBlank
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
