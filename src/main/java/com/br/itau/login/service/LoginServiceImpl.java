package com.br.itau.login.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.exception.UserNotFoundException;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.utils.SessionUtils;

@Service
public class LoginServiceImpl implements LoginService {

	private final AuthenticationManager authenticationManager;
	private final SessionUtils sessionUtils;
	private final UserRepositoryDomain userRepositoryPort;
	private final SessionService sessionService;

	public LoginServiceImpl(AuthenticationManager authenticationManager, SessionUtils sessionUtils,
			UserRepositoryDomain userRepositoryPort, SessionService sessionService) {
		this.authenticationManager = authenticationManager;
		this.sessionUtils = sessionUtils;
		this.userRepositoryPort = userRepositoryPort;
		this.sessionService = sessionService;
	}

	@Override
	public AuthResponse login(LoginRequest loginRequest) {

		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		UserAccount userAccount = userRepositoryPort.findByUsername(loginRequest.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		String roleName = userAccount.getRole() != null ? userAccount.getRole().name() : null;

		// Single-session enforcement: invalidate existing session if present
		String existingSessionId = sessionService.findSessionIdByUserId(userAccount.getId());
		if (existingSessionId != null) {
			sessionService.delete(existingSessionId);
		}
		// Remove existing refresh token for this user
		String existingRefreshToken = sessionService.findRefreshTokenByUserId(userAccount.getId());
		if (existingRefreshToken != null) {
			sessionService.deleteRefreshToken(existingRefreshToken);
			sessionService.deleteUserRefreshToken(userAccount.getId());
		}
		sessionService.deleteUserSession(userAccount.getId());

		String sessionId = sessionUtils.generateSessionId();
		String symmetricKey = sessionUtils.generateSymmetricKey();

		SessionDTO session = new SessionDTO(sessionId, loginRequest.getUsername(), userAccount.getContractService(),
				symmetricKey, roleName, userAccount.getId(), userAccount.getEscolaId());

		sessionUtils.saveSession(session);
		String token = sessionUtils.createToken(loginRequest, sessionId, roleName, userAccount);
		String refreshToken = sessionUtils.saveRefreshToken(userAccount.getId());

		return new AuthResponse(token, refreshToken);
	}

}
