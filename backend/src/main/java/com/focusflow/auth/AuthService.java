package com.focusflow.auth;

import com.focusflow.auth.dto.LoginRequest;
import com.focusflow.auth.dto.RegisterRequest;
import com.focusflow.auth.dto.UserResponse;
import com.focusflow.common.error.ConflictException;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new ConflictException("email already registered");
		}
		if (userRepository.findByUsername(request.username()).isPresent()) {
			throw new ConflictException("username already taken");
		}

		User user = new User();
		user.setEmail(request.email());
		user.setUsername(request.username());
		user.setPasswordHash(passwordEncoder.encode(request.password()));

		User saved = userRepository.save(user);
		return new UserResponse(saved.getId(), saved.getEmail(), saved.getUsername());
	}

	public UserResponse login(LoginRequest request) {
		Authentication authentication =
				authenticationManager.authenticate(
						new UsernamePasswordAuthenticationToken(
								request.username(), request.password()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		User user =
				userRepository
						.findByUsername(request.username())
						.orElseThrow();
		return new UserResponse(user.getId(), user.getEmail(), user.getUsername());
	}
}
