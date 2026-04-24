package com.br.itau.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

import jakarta.validation.Valid;

@RequestMapping("/auth")
public interface Login {

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid LoginRequest loginRequest);
}
