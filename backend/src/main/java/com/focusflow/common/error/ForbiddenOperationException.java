package com.focusflow.common.error;

public class ForbiddenOperationException extends RuntimeException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
