package com.focusflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.focusflow.auth.dto.RegisterRequest;
import com.focusflow.security.CurrentUser;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private CurrentUser currentUser;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService =
				new AuthService(
						userRepository, passwordEncoder, authenticationManager, currentUser);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void register_whenValid_setsAuthenticatedSecurityContext() {
		RegisterRequest request =
				new RegisterRequest("new@example.com", "newuser", "password123");

		when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("password123")).thenReturn("hashed");

		User saved = new User();
		saved.setEmail("new@example.com");
		saved.setUsername("newuser");
		saved.setPasswordHash("hashed");
		ReflectionTestUtils.setField(saved, "id", 2L);
		when(userRepository.save(any(User.class))).thenReturn(saved);

		Authentication authentication = mock(Authentication.class);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);

		authService.register(request);

		assertThat(SecurityContextHolder.getContext().getAuthentication())
				.isSameAs(authentication);
	}

	@Test
	void register_whenValid_authenticatesWithRequestCredentials() {
		RegisterRequest request =
				new RegisterRequest("new@example.com", "newuser", "password123");

		when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("password123")).thenReturn("hashed");

		User saved = new User();
		saved.setEmail("new@example.com");
		saved.setUsername("newuser");
		saved.setPasswordHash("hashed");
		ReflectionTestUtils.setField(saved, "id", 2L);
		when(userRepository.save(any(User.class))).thenReturn(saved);

		Authentication authentication = mock(Authentication.class);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);

		authService.register(request);

		ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(captor.capture());
		Authentication token = captor.getValue();
		assertThat(token).isInstanceOf(UsernamePasswordAuthenticationToken.class);
		assertThat(token.getName()).isEqualTo("newuser");
		assertThat(token.getCredentials()).isEqualTo("password123");
	}
}
