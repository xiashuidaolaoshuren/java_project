package com.focusflow.ai;

import com.focusflow.plan.RankingRejectionReason;

public class AiProviderException extends RuntimeException {

	private final RankingRejectionReason reason;

	public AiProviderException(String message) {
		this(message, null, null);
	}

	public AiProviderException(String message, Throwable cause) {
		this(message, cause, null);
	}

	public AiProviderException(String message, RankingRejectionReason reason) {
		super(message);
		this.reason = reason;
	}

	public AiProviderException(String message, Throwable cause, RankingRejectionReason reason) {
		super(message, cause);
		this.reason = reason;
	}

	public RankingRejectionReason getReason() {
		return reason;
	}
}
