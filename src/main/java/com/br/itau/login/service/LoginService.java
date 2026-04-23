package com.br.itau.login.service;

import org.springframework.http.ResponseEntity;

import com.br.itau.login.model.request.LoginRequest;

public interface LoginService {

	ResponseEntity login(LoginRequest loginRequest);
}
