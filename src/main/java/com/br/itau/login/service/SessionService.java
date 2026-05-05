package com.br.itau.login.service;

import com.br.itau.login.model.SessionDTO;

public interface SessionService {
	SessionDTO find(String sessionId);
	void delete(String sessionId);
	void save(SessionDTO session, long ttlMillis);

	// User-level session tracking (single active session per user)
	void saveUserSession(Long userId, String sessionId, long ttlMillis);
	String findSessionIdByUserId(Long userId);
	void deleteUserSession(Long userId);

	// Refresh token management (token UUID → userId)
	void saveRefreshToken(String refreshToken, Long userId, long ttlMillis);
	Long findUserIdByRefreshToken(String refreshToken);
	void deleteRefreshToken(String refreshToken);

	// User → refresh token reverse lookup for logout cleanup
	void saveUserRefreshToken(Long userId, String refreshToken, long ttlMillis);
	String findRefreshTokenByUserId(Long userId);
	void deleteUserRefreshToken(Long userId);
}
