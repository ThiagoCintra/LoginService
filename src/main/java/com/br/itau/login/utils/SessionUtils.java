package com.br.itau.login.utils;

import org.springframework.stereotype.Component;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.service.JwtService;
import com.br.itau.login.service.SessionService;

@Component
public class SessionUtils {

	private final SessionService sessionService;
	private final JwtService jwtService;

	public SessionUtils(SessionService sessionService, JwtService jwtService) {
		this.sessionService = sessionService;
		this.jwtService = jwtService;
	}

	public String generateSessionId() {
		return java.util.UUID.randomUUID().toString();
	}

	public String generateSymmetricKey() {
		byte[] keyBytes = new byte[32];
		new java.security.SecureRandom().nextBytes(keyBytes);
		return java.util.Base64.getEncoder().encodeToString(keyBytes);
	}

	public String generateRefreshToken() {
		return java.util.UUID.randomUUID().toString();
	}

	public void saveSession(SessionDTO session) {
		sessionService.save(session, jwtService.getExpirationMs());
		if (session.getUserId() != null) {
			sessionService.saveUserSession(session.getUserId(), session.getSessionId(), jwtService.getExpirationMs());
		}
	}

	public String saveRefreshToken(Long userId) {
		String refreshToken = generateRefreshToken();
		long ttl = jwtService.getRefreshExpirationMs();
		sessionService.saveRefreshToken(refreshToken, userId, ttl);
		sessionService.saveUserRefreshToken(userId, refreshToken, ttl);
		return refreshToken;
	}

	public String createToken(LoginRequest loginRequest, String sessionId, String roleName, UserAccount userAccount) {
		return jwtService.generateToken(loginRequest.getUsername(), sessionId, roleName,
				userAccount.getContractService(), userAccount.getEscolaId());
	}

	public String createToken(String username, String sessionId, String roleName, Boolean contractService, Long escolaId) {
		return jwtService.generateToken(username, sessionId, roleName, contractService, escolaId);
	}
}
