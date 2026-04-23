package com.br.itau.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.service.LoginService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth2")
public class LoginImpl implements Login {

	private final LoginService loginService;
	private final Logger logger = LoggerFactory.getLogger(LoginImpl.class);

	public LoginImpl(LoginService loginService) {
		this.loginService = loginService;
	}

	@Override
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		logger.info("Login request for user={}", loginRequest.getUsername());
		try {
			return loginService.login(loginRequest);
		} catch (Throwable t) {
			logger.error("Error while processing login", t);
			throw t;
		}
	}

}
