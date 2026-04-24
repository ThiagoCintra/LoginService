package com.br.itau.login.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
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

	public LoginServiceImpl(AuthenticationManager authenticationManager, SessionUtils sessionUtils,
			UserRepositoryDomain userRepositoryPort) {
		this.authenticationManager = authenticationManager;
		this.sessionUtils = sessionUtils;
		this.userRepositoryPort = userRepositoryPort;
	}

	@Override
	public AuthResponse login(LoginRequest loginRequest) {

		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		UserAccount userAccount = userRepositoryPort.findByUsername(loginRequest.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		String roleName = userAccount != null && userAccount.getRole() != null ? userAccount.getRole().name() : null;

		String sessionId = sessionUtils.generateSessionId();

		String symmetricKey = sessionUtils.generateSymmetricKey();

		SessionDTO session = new SessionDTO(sessionId, loginRequest.getUsername(), userAccount.getContractService(),
				symmetricKey, roleName);

		sessionUtils.saveSession(session);
		String token = sessionUtils.createToken(loginRequest, sessionId, roleName, userAccount);

		return new AuthResponse(token);

	}

}
