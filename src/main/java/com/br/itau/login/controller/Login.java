package com.br.itau.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.request.RefreshRequest;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.model.response.MeResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RequestMapping("/auth")
public interface Login {

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid LoginRequest loginRequest);

	@GetMapping("/me")
	ResponseEntity<MeResponseDTO> me(@AuthenticationPrincipal SessionDTO session);

	@PostMapping("/logout")
	ResponseEntity<java.util.Map<String, String>> logout(@RequestHeader("Authorization") String authorizationHeader);

	@PostMapping("/refresh")
	ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest body, HttpServletRequest request);

}
