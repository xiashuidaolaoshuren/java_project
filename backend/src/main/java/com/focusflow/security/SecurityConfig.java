package com.focusflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider authenticationProvider(
			FocusFlowUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
			throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain filterChain(
			HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
		CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
		csrfRequestHandler.setCsrfRequestAttributeName(null);

		return http.authenticationProvider(authenticationProvider)
				.authorizeHttpRequests(
						auth ->
								auth.requestMatchers("/api/auth/register", "/api/auth/login")
										.permitAll()
										.requestMatchers(
												"/actuator/health/liveness",
												"/actuator/health/readiness")
										.permitAll()
										.anyRequest()
										.authenticated())
				.exceptionHandling(
						ex ->
								ex.authenticationEntryPoint(
										new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.csrf(
						csrf ->
								csrf.csrfTokenRepository(
												CookieCsrfTokenRepository.withHttpOnlyFalse())
										.csrfTokenRequestHandler(csrfRequestHandler)
										.ignoringRequestMatchers(
												"/actuator/health/liveness",
												"/actuator/health/readiness"))
				.securityContext(ctx -> ctx.requireExplicitSave(false))
				.logout(
						logout ->
								logout.logoutUrl("/api/auth/logout")
										.logoutSuccessHandler(
												new HttpStatusReturningLogoutSuccessHandler(
														HttpStatus.NO_CONTENT))
										.invalidateHttpSession(true)
										.deleteCookies("JSESSIONID")
										.clearAuthentication(true))
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.build();
	}
}
