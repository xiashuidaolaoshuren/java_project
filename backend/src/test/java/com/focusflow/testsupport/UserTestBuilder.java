package com.focusflow.testsupport;

import com.focusflow.user.User;
import java.util.UUID;

/**
 * Fluent builder for {@link User} in tests. Defaults produce a unique email/username pair.
 */
public final class UserTestBuilder {

	private String unique = UUID.randomUUID().toString().substring(0, 8);
	private String accountPrefix = "user";
	private String emailOverride;
	private String usernameOverride;
	private String passwordHash =
			"$2a$10$hashedPlaceholderForBcryptLater";

	private UserTestBuilder() {}

	public static UserTestBuilder user() {
		return new UserTestBuilder();
	}

	public UserTestBuilder withUnique(String unique) {
		this.unique = unique;
		return this;
	}

	/** Base name used when email/username are not overridden: {@code prefix-unique@example.com}. */
	public UserTestBuilder withAccountPrefix(String accountPrefix) {
		this.accountPrefix = accountPrefix;
		return this;
	}

	public UserTestBuilder withEmail(String email) {
		this.emailOverride = email;
		return this;
	}

	public UserTestBuilder withUsername(String username) {
		this.usernameOverride = username;
		return this;
	}

	public UserTestBuilder withPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
		return this;
	}

	public User build() {
		User user = new User();
		user.setEmail(emailOverride != null ? emailOverride : accountPrefix + "-" + unique + "@example.com");
		user.setUsername(usernameOverride != null ? usernameOverride : accountPrefix + "-" + unique);
		user.setPasswordHash(passwordHash);
		return user;
	}
}
