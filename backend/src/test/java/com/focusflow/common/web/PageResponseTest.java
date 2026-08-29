package com.focusflow.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

	@Test
	void exposesPageEnvelopeFields() {
		PageResponse<String> response = new PageResponse<>(List.of("alpha"), 0, 20, 1L, 1);

		assertThat(response.content()).containsExactly("alpha");
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(1L);
		assertThat(response.totalPages()).isEqualTo(1);
	}
}
