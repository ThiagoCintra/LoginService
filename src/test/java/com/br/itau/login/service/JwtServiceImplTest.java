package com.br.itau.login.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;

class JwtServiceImplTest {

	private static final String SECRET = "TestSecretKeyForUnitTestingAtLeast32Chars!!";
	private static final long EXPIRATION_MS = 60_000L;

	private JwtServiceImpl jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtServiceImpl(SECRET, EXPIRATION_MS);
	}

	@Test
	void generateToken_shouldReturnNonNullToken() {
		String token = jwtService.generateToken("user1", "session-1", "USER", Boolean.FALSE, "WEB");
		assertThat(token).isNotBlank();
	}

	@Test
	void isTokenValid_shouldReturnTrueForValidToken() {
		String token = jwtService.generateToken("user1", "session-1", "USER", Boolean.TRUE, "WEB");
		assertThat(jwtService.isTokenValid(token)).isTrue();
	}

	@Test
	void isTokenValid_shouldReturnFalseForTamperedToken() {
		String token = jwtService.generateToken("user1", "session-1", "USER", Boolean.FALSE, "WEB");
		String tampered = token.substring(0, token.length() - 4) + "XXXX";
		assertThat(jwtService.isTokenValid(tampered)).isFalse();
	}

	@Test
	void getClaims_shouldContainExpectedClaims() {
		String token = jwtService.generateToken("user1", "session-42", "ADMIN", Boolean.TRUE, "WEB");
		Claims claims = jwtService.getClaims(token);

		assertThat(claims.getSubject()).isEqualTo("user1");
		assertThat(claims.get("sessionId", String.class)).isEqualTo("session-42");
		assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
		assertThat(claims.get("contractService", Boolean.class)).isTrue();
	}

	@Test
	void getExpirationMs_shouldReturnConfiguredValue() {
		assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
	}
}
