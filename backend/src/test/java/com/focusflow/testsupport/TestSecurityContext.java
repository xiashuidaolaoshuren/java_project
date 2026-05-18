package com.focusflow.testsupport;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Minimal helpers for setting the {@link SecurityContextHolder} in unit/service tests.
 * Prefer {@code @WithMockUser} for Spring MVC tests when security applies.
 */
public final class TestSecurityContext {

	public record AuthenticatedUser(long id, String username) {}

	private TestSecurityContext() {}

	public static void setAuthenticated(long userId, String username) {
		setAuthenticated(new AuthenticatedUser(userId, username), java.util.List.of());
	}

	public static void setAuthenticated(AuthenticatedUser user, Collection<String> roleNames) {
		var authorities = roleNames.stream().map(SimpleGrantedAuthority::new).toList();
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(user, "n/a", authorities);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	public static void clear() {
		SecurityContextHolder.clearContext();
	}

	/** Run {@code runnable} with an authenticated context, then clear. */
	public static void runAs(long userId, String username, Runnable runnable) {
		setAuthenticated(userId, username);
		try {
			runnable.run();
		} finally {
			clear();
		}
	}
}
