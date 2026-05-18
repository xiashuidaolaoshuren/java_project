package com.focusflow.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class TestSecurityContextTest {

	@Test
	void setAuthenticated_thenClear_updatesAndClearsContext() {
		TestSecurityContext.setAuthenticated(99L, "fixture-user");

		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.isEqualTo(new TestSecurityContext.AuthenticatedUser(99L, "fixture-user"));

		TestSecurityContext.clear();

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void runAs_executesThenClears() {
		TestSecurityContext.runAs(
				1L,
				"u",
				() ->
						assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
								.isEqualTo(new TestSecurityContext.AuthenticatedUser(1L, "u")));

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
