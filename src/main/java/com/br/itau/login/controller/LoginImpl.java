package com.br.itau.login.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.service.LoginService;

import jakarta.validation.Valid;

public class LoginImpl implements Login {

	private LoginService loginService;

	@Autowired
	private LoginImpl (LoginService loginService) {
		this.loginService = loginService;
		
	}
	
	@Override
	public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest) {
		loginService.login(loginRequest);
		return ResponseEntity.ok("");
	}

}
