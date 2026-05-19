package com.focusflow.auth;

import com.focusflow.auth.dto.RegisterRequest;
import com.focusflow.auth.dto.UserResponse;
import com.focusflow.common.error.ConflictException;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
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
}
