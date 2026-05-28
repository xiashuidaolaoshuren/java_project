package com.focusflow.security;

import com.focusflow.common.error.NotFoundException;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	private final UserRepository userRepository;

	public CurrentUser(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserContext getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new NotFoundException("not authenticated");
		}

		String username = authentication.getName();
		User user =
				userRepository
						.findByUsername(username)
						.orElseThrow(() -> new NotFoundException("user not found"));
		return new UserContext(user.getId(), user.getEmail(), user.getUsername());
	}
}
