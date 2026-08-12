package com.focusflow.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

class CsrfCookieFilterTest {

	@Test
	void resolvesCsrfTokenWhenPresent() throws ServletException, IOException {
		CsrfCookieFilter filter = new CsrfCookieFilter();
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);
		CsrfToken csrfToken = mock(CsrfToken.class);

		when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);

		filter.doFilter(request, response, filterChain);

		verify(csrfToken).getToken();
		verify(filterChain).doFilter(request, response);
	}
}
