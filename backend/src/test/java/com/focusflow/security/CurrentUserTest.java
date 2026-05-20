package com.focusflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.focusflow.auth.dto.UserResponse;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CurrentUser currentUser;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getCurrentUser_whenAuthenticated_returnsUserResponse() {
		SecurityContextHolder.getContext()
				.setAuthentication(
						new UsernamePasswordAuthenticationToken("alice", "n/a", java.util.List.of()));

		User user = new User();
		user.setEmail("alice@example.com");
		user.setUsername("alice");
		user.setPasswordHash("$2a$10$hash");
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		UserResponse response = currentUser.getCurrentUser();

		assertThat(response.email()).isEqualTo("alice@example.com");
		assertThat(response.username()).isEqualTo("alice");
	}
}
