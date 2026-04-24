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
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

import static com.br.itau.login.utils.SessionUtils.*;

@Service
public class LoginServiceImpl implements LoginService {

	private final AuthenticationManager authenticationManager;
	
	private final UserRepositoryDomain userRepositoryPort;

	public LoginServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService,
			SessionService sessionService, UserRepositoryDomain userRepositoryPort) {
		this.authenticationManager = authenticationManager;
		this.userRepositoryPort = userRepositoryPort;
	}

	@Override
	public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {

		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		UserAccount userAccount = userRepositoryPort.findByUsername(loginRequest.getUsername()).orElse(null);
		String roleName = userAccount != null && userAccount.getRole() != null ? userAccount.getRole().name() : null;

		String sessionId = geraneteSessionId();
		
		String symmetricKey = generateSymmetricKey();

		if (Objects.nonNull(userAccount)) {
			SessionDTO	session = new SessionDTO(sessionId, loginRequest.getUsername(), userAccount.getContractService(),
					symmetricKey, roleName);
			
			saveSession(session);
			String token = getSession(loginRequest, sessionId, roleName, userAccount);

			return ResponseEntity.ok(new AuthResponse(token));
		}else {
			throw new RuntimeException("User not found");
		}

	}

}
