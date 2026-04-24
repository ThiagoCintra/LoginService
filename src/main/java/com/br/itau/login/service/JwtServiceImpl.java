package com.br.itau.login.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

	private final Key key;
	private final long expirationMs;

	public JwtServiceImpl(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
		this.expirationMs = expirationMs;
	}

	@Override
	public String generateToken(String username, String sessionId, String role, Boolean contractService) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationMs);
		return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(exp).claim("sessionId", sessionId)
				.claim("role", role).claim("contractService", contractService).signWith(key, SignatureAlgorithm.HS256).compact();
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			parseToken(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	@Override
	public Claims getClaims(String token) {
		return parseToken(token).getBody();
	}

	private Jws<Claims> parseToken(String token) throws JwtException {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
	}

	public long getExpirationMs() {
		return expirationMs;
	}

}
