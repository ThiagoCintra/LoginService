package com.br.itau.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.enums.Role;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

class LoginServiceImplTest {

	private AuthenticationManager authenticationManager;
	private JwtService jwtService;
	private SessionService sessionService;
	private UserRepositoryDomain userRepositoryPort;
	private LoginServiceImpl loginService;

	@BeforeEach
	void setUp() {
		authenticationManager = mock(AuthenticationManager.class);
		jwtService = mock(JwtService.class);
		sessionService = mock(SessionService.class);
		userRepositoryPort = mock(UserRepositoryDomain.class);
		com.br.itau.login.utils.SessionUtils sessionUtils = new com.br.itau.login.utils.SessionUtils(sessionService, jwtService);
		loginService = new LoginServiceImpl(authenticationManager, sessionUtils, userRepositoryPort);
	}

	@Test
	void login_shouldReturnTokenWhenCredentialsAreValid() {
		LoginRequest request = new LoginRequest("Thiago", "231299");

		Authentication auth = mock(Authentication.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

		UserAccount account = new UserAccount();
		account.setUsername("Thiago");
		account.setPassword("encoded");
		account.setRole(Role.USER);
		account.setContractService(Boolean.FALSE);
		when(userRepositoryPort.findByUsername("Thiago")).thenReturn(Optional.of(account));

		when(jwtService.getExpirationMs()).thenReturn(60_000L);
		when(jwtService.generateToken(any(), any(), any(), any(), any())).thenReturn("jwt-token");

		AuthResponse response = loginService.login(request, "WEB");
		
		ResponseEntity<AuthResponse> responseEntity = ResponseEntity.ok(response);

		assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
		assertThat(responseEntity.getBody()).isNotNull();
		assertThat(responseEntity.getBody().getToken()).isEqualTo("jwt-token");

		verify(sessionService).save(any(), any(Long.class));
	}

	@Test
	void login_shouldThrowWhenUserNotFoundInRepository() {
		LoginRequest request = new LoginRequest("unknown", "pass");

		Authentication auth = mock(Authentication.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
		when(userRepositoryPort.findByUsername("unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> loginService.login(request, "WEB"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("User not found");
	}

	@Test
	void login_shouldThrowWhenAuthenticationFails() {
		LoginRequest request = new LoginRequest("Thiago", "wrongpass");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		assertThatThrownBy(() -> loginService.login(request, "WEB"))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void login_shouldHandleNullRole() {
		LoginRequest request = new LoginRequest("Thiago", "231299");

		Authentication auth = mock(Authentication.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

		UserAccount account = new UserAccount();
		account.setUsername("Thiago");
		account.setPassword("encoded");
		account.setRole(null);
		account.setContractService(Boolean.TRUE);
		when(userRepositoryPort.findByUsername("Thiago")).thenReturn(Optional.of(account));

		when(jwtService.getExpirationMs()).thenReturn(60_000L);
		when(jwtService.generateToken(any(), any(), any(), any(), any())).thenReturn("jwt-token-no-role");

		AuthResponse response = loginService.login(request, "WEB");
		
		ResponseEntity<AuthResponse> responseEntity = ResponseEntity.ok(response);

		assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
		assertThat(responseEntity.getBody().getToken()).isEqualTo("jwt-token-no-role");
	}
}
