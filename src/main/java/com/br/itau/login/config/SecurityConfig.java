package com.br.itau.login.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.br.itau.login.security.JwtAuthenticationFilter;
import com.br.itau.login.security.LoginRateLimitFilter;
import com.br.itau.login.security.ContractAuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final LoginRateLimitFilter loginRateLimitFilter;
	private final ContractAuthorizationFilter contractAuthorizationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, LoginRateLimitFilter loginRateLimitFilter,
						  ContractAuthorizationFilter contractAuthorizationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.loginRateLimitFilter = loginRateLimitFilter;
		this.contractAuthorizationFilter = contractAuthorizationFilter;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/auth/login", "/auth/refresh", "/actuator/health")
						.permitAll().anyRequest().authenticated())
				
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
						response.sendError(401, "Unauthorized"))
						.accessDeniedHandler((request, response, accessDeniedException) ->
						response.sendError(403, "Forbidden")))

				.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(contractAuthorizationFilter, JwtAuthenticationFilter.class);
		return http.build();
	}
}
