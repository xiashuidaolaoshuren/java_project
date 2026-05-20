package com.focusflow.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focusflow.auth.dto.LoginRequest;
import com.focusflow.auth.dto.RegisterRequest;
import com.focusflow.auth.dto.UserResponse;
import com.focusflow.common.error.ConflictException;
import com.focusflow.common.error.GlobalExceptionHandler;
import com.focusflow.security.FocusFlowUserDetailsService;
import com.focusflow.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;

	@MockBean
	private FocusFlowUserDetailsService userDetailsService;

	@Test
	void login_withValidCredentials_returns200AndUserResponse() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenReturn(new UserResponse(1L, "alice@example.com", "alice"));

		mockMvc.perform(
						post("/api/auth/login")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "username": "alice",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.username").value("alice"));
	}

	@Test
	void login_withWrongPassword_returns401() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(
						post("/api/auth/login")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "username": "alice",
										  "password": "wrong"
										}
										"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid credentials"))
				.andExpect(jsonPath("$.path").value("/api/auth/login"));
	}

	@Test
	void login_withUnknownUsername_returns401() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(
						post("/api/auth/login")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "username": "nobody",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid credentials"))
				.andExpect(jsonPath("$.path").value("/api/auth/login"));
	}

	@Test
	@WithMockUser
	void logout_whenAuthenticated_returns204() throws Exception {
		mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void me_whenUnauthenticated_returns403() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());

		verify(authService, never()).getCurrentUser();
	}

	@Test
	@WithMockUser
	void me_whenAuthenticated_returns200AndUserResponse() throws Exception {
		when(authService.getCurrentUser())
				.thenReturn(new UserResponse(1L, "alice@example.com", "alice"));

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.username").value("alice"));
	}

	@Test
	void register_withValidData_returns201AndUserResponse() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenReturn(new UserResponse(1L, "alice@example.com", "alice"));

		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "alice@example.com",
										  "username": "alice",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.username").value("alice"));
	}

	@Test
	void register_withDuplicateEmail_returns409() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenThrow(new ConflictException("email already registered"));

		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "dup@example.com",
										  "username": "user1",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("email already registered"))
				.andExpect(jsonPath("$.path").value("/api/auth/register"));
	}

	@Test
	void register_withDuplicateUsername_returns409() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenThrow(new ConflictException("username already taken"));

		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "user2@example.com",
										  "username": "taken",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("username already taken"))
				.andExpect(jsonPath("$.path").value("/api/auth/register"));
	}

	@Test
	void register_withBlankEmail_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "",
										  "username": "alice",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.path").value("/api/auth/register"))
				.andExpect(jsonPath("$.details.email").isArray());

		verify(authService, never()).register(any(RegisterRequest.class));
	}

	@Test
	void register_withBlankUsername_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "alice@example.com",
										  "username": "",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.username").isArray());

		verify(authService, never()).register(any(RegisterRequest.class));
	}

	@Test
	void register_withBlankPassword_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "alice@example.com",
										  "username": "alice",
										  "password": ""
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.password").isArray());

		verify(authService, never()).register(any(RegisterRequest.class));
	}

	@Test
	void register_withInvalidEmailFormat_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "email": "not-an-email",
										  "username": "alice",
										  "password": "password123"
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.email").isArray());

		verify(authService, never()).register(any(RegisterRequest.class));
	}
}
