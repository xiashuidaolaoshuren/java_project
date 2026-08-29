package com.focusflow.ai;

import java.time.Duration;

@FunctionalInterface
public interface Sleeper {

	void sleep(Duration duration) throws InterruptedException;
}
