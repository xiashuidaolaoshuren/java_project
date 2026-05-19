package com.focusflow.security;

import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.util.Collections;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FocusFlowUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public FocusFlowUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user =
				userRepository
						.findByUsername(username)
						.orElseThrow(
								() -> new UsernameNotFoundException("User not found: " + username));
		return new org.springframework.security.core.userdetails.User(
				user.getUsername(), user.getPasswordHash(), Collections.emptyList());
	}
}
