package com.br.itau.login.controller;

import org.springframework.http.ResponseEntity;

import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

import jakarta.validation.Valid;

public interface Login {

	ResponseEntity<AuthResponse> login(@Valid LoginRequest loginRequest);
}
