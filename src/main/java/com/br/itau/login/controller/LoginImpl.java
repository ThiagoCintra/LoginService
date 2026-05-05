package com.br.itau.login.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.request.RefreshRequest;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.model.response.MeResponseDTO;
import com.br.itau.login.service.LoginService;
import com.br.itau.login.service.LogoutService;
import com.br.itau.login.service.MeService;
import com.br.itau.login.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class LoginImpl implements Login {

	private final LoginService loginService;
	private final MeService meService;
	private final LogoutService logoutService;
	private final RefreshTokenService refreshTokenService;

	public LoginImpl(LoginService loginService, MeService meService,
			LogoutService logoutService, RefreshTokenService refreshTokenService) {
		this.loginService = loginService;
		this.meService = meService;
		this.logoutService = logoutService;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		return ResponseEntity.ok(loginService.login(loginRequest));
	}

	@Override
	public ResponseEntity<MeResponseDTO> me(@AuthenticationPrincipal SessionDTO session) {
		if (session == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		MeResponseDTO dto = meService.getUserInfo(session.getSessionId(), session.getSessionId(), session.getUsername());
		dto.setChannel("MOBILE");
		return ResponseEntity.ok(dto);
	}

	@Override
	public ResponseEntity<java.util.Map<String, String>> logout(String authorizationHeader) {
		String token = null;
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			token = authorizationHeader.substring(7);
		}
		logoutService.logout(token);
		return ResponseEntity.ok(java.util.Map.of("message", "Logout realizado"));
	}

	@Override
	public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest refreshRequest,
			HttpServletRequest request) {
		String refreshToken = resolveRefreshToken(refreshRequest, request);
		AuthResponse response = refreshTokenService.refresh(refreshToken);
		return ResponseEntity.ok(response);
	}

	private String resolveRefreshToken(RefreshRequest refreshRequest, HttpServletRequest request) {
		// 1. Try request body
		if (refreshRequest != null && refreshRequest.getRefreshToken() != null && !refreshRequest.getRefreshToken().isBlank()) {
			return refreshRequest.getRefreshToken();
		}
		// 2. Try cookie
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("refreshToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}
