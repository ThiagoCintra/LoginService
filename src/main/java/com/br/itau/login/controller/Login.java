package com.br.itau.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.request.LoginRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public interface Login {

	
	@PostMapping("/login")
	ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest);
}
