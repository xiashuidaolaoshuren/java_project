package com.focusflow.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class RequestIdFilterTest {

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void echoesValidInboundRequestIdAndSetsMdcDuringChain() throws ServletException, IOException {
		RequestIdFilter filter = new RequestIdFilter();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);
		ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

		when(request.getHeader("X-Request-Id")).thenReturn("abc-123");

		filter.doFilter(request, response, filterChain);

		verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Request-Id"), headerCaptor.capture());
		assertThat(headerCaptor.getValue()).isEqualTo("abc-123");
		assertThat(MDC.get("requestId")).isNull();
	}

	@Test
	void generatesUuidWhenRequestIdAbsent() throws ServletException, IOException {
		RequestIdFilter filter = new RequestIdFilter();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);
		ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

		when(request.getHeader("X-Request-Id")).thenReturn(null);

		filter.doFilter(request, response, filterChain);

		verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Request-Id"), headerCaptor.capture());
		assertThat(headerCaptor.getValue()).matches(
				"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		assertThat(MDC.get("requestId")).isNull();
	}

	@Test
	void replacesInvalidRequestIdWithUuid() throws ServletException, IOException {
		RequestIdFilter filter = new RequestIdFilter();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);
		ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

		when(request.getHeader("X-Request-Id")).thenReturn("bad id!");

		filter.doFilter(request, response, filterChain);

		verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Request-Id"), headerCaptor.capture());
		assertThat(headerCaptor.getValue()).isNotEqualTo("bad id!");
		assertThat(headerCaptor.getValue()).matches(
				"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}

	@Test
	void setsMdcDuringFilterChainAndClearsAfter() throws ServletException, IOException {
		RequestIdFilter filter = new RequestIdFilter();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		when(request.getHeader("X-Request-Id")).thenReturn("abc-123");
		org.mockito.Mockito.doAnswer(
						invocation -> {
							assertThat(MDC.get("requestId")).isEqualTo("abc-123");
							return null;
						})
				.when(filterChain)
				.doFilter(request, response);

		filter.doFilter(request, response, filterChain);

		assertThat(MDC.get("requestId")).isNull();
	}
}
