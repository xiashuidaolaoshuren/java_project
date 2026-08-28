package com.focusflow.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {

	static final String REQUEST_ID_HEADER = "X-Request-Id";
	static final String MDC_REQUEST_ID_KEY = "requestId";
	private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
		response.setHeader(REQUEST_ID_HEADER, requestId);
		MDC.put(MDC_REQUEST_ID_KEY, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_REQUEST_ID_KEY);
		}
	}

	private String resolveRequestId(String inbound) {
		if (inbound != null && VALID_REQUEST_ID.matcher(inbound).matches()) {
			return inbound;
		}
		return UUID.randomUUID().toString();
	}
}
