package com.br.itau.login.controller.contract;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.br.itau.login.model.response.AuthResponse;

@RequestMapping("/contract")
public interface ContractController{
	
	@PostMapping
	ResponseEntity contract();
}
