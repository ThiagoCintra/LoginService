package com.br.itau.login.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
	String generateToken(String username, String sessionId, String role, Boolean contractService, Long escolaId);
	boolean isTokenValid(String token);
	Claims getClaims(String token);
	long getExpirationMs();
	long getRefreshExpirationMs();
}
