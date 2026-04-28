package com.br.itau.login.service;

import org.springframework.http.ResponseEntity;

import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;

public interface LoginService {

	AuthResponse login(LoginRequest loginRequest, String channel);
}
