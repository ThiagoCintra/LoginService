package com.br.itau.login.utils;

import org.springframework.stereotype.Component;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.service.JwtService;
import com.br.itau.login.service.SessionService;

@Component
public class SessionUtils {

	private static SessionService sessionService;
	private static JwtService jwtService;

	public SessionUtils(SessionService sessionService, JwtService jwtService) {
		SessionUtils.sessionService = sessionService;
		SessionUtils.jwtService = jwtService;
	}

	// Expose setters so tests or manual constructions can inject mocks into the static helpers
	public static void setSessionService(SessionService sessionService) {
		SessionUtils.sessionService = sessionService;
	}

	public static void setJwtService(JwtService jwtService) {
		SessionUtils.jwtService = jwtService;
	}

	public static void setServices(SessionService sessionService, JwtService jwtService) {
		SessionUtils.sessionService = sessionService;
		SessionUtils.jwtService = jwtService;
	}

	public static String geraneteSessionId() {
		return java.util.UUID.randomUUID().toString();
	}

	public static String generateSymmetricKey() {
		byte[] keyBytes = new byte[32];
		new java.security.SecureRandom().nextBytes(keyBytes);
		return java.util.Base64.getEncoder().encodeToString(keyBytes);
	}

	public static void saveSession(SessionDTO session) {
		sessionService.save(session, jwtService.getExpirationMs());
	}

	public static String getSession(LoginRequest loginRequest, String sessionId, String roleName,
			UserAccount userAccount) {
		return jwtService.generateToken(loginRequest.getUsername(), sessionId, roleName,
				userAccount.getContractService());
	}
}
