package com.focusflow.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, List<String>> details) {

	public static ApiErrorResponse of(
			int status,
			String error,
			String message,
			String path,
			Map<String, List<String>> details) {
		return new ApiErrorResponse(Instant.now(), status, error, message, path, details);
	}

	public static ApiErrorResponse withoutDetails(int status, String error, String message, String path) {
		return of(status, error, message, path, null);
	}
}
