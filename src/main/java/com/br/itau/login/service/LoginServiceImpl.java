package com.br.itau.login.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

@Service
public class LoginServiceImpl implements LoginService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final SessionService sessionService;
	private final UserRepositoryDomain userRepositoryPort;

	public LoginServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService,
			SessionService sessionService, UserRepositoryDomain userRepositoryPort) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.sessionService = sessionService;
		this.userRepositoryPort = userRepositoryPort;
	}

	@Override
	public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {

		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		UserAccount userAccount = userRepositoryPort.findByUsername(loginRequest.getUsername()).orElse(null);
		String roleName = userAccount != null && userAccount.getRole() != null ? userAccount.getRole().name() : null;

		String sessionId = java.util.UUID.randomUUID().toString();
		byte[] keyBytes = new byte[32];
		new SecureRandom().nextBytes(keyBytes);
		String symmetricKey = Base64.getEncoder().encodeToString(keyBytes);
		SessionDTO session = null;

		if (userAccount != null) {
			session = new SessionDTO(sessionId, loginRequest.getUsername(), userAccount.getContractService(),
					symmetricKey, roleName);
			sessionService.save(session, jwtService.getExpirationMs());

			String token = jwtService.generateToken(loginRequest.getUsername(), sessionId, roleName,
					userAccount.getContractService());

			return ResponseEntity.ok(new AuthResponse(token));
		}else {
			throw new RuntimeException("User not found");
		}

		

	}

}
