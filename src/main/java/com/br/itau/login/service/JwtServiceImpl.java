package com.br.itau.login.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

	private final Key key;
	private final long expirationMs;
	private final long refreshExpirationMs;

	public JwtServiceImpl(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-ms}") long expirationMs,
			@Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
		this.expirationMs = expirationMs;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	@Override
	public String generateToken(String username, String sessionId, String role, Boolean contractService, Long escolaId) {
	    Date now = new Date();
	    Date exp = new Date(now.getTime() + expirationMs);
	    return Jwts.builder()
	            .setSubject(username)
	            .setIssuedAt(now)
	            .setExpiration(exp)
	            .claim("sessionId", sessionId)
	            .claim("role", role)
	            .claim("contractService", contractService)
	            .claim("channel", "MOBILE")
	            .claim("escolaId", escolaId)
	            .signWith(key, SignatureAlgorithm.HS256)
	            .compact();
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

	@Override
	public long getExpirationMs() {
		return expirationMs;
	}

	@Override
	public long getRefreshExpirationMs() {
		return refreshExpirationMs;
	}

}
