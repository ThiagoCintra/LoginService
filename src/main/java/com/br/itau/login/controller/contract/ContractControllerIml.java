package com.br.itau.login.controller.contract;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.service.ContractService;

@RestController
public class ContractControllerIml implements ContractController {

	private final ContractService contractService;

	public ContractControllerIml(ContractService contractService) {
		this.contractService = contractService;
	}

	@Override
	public ResponseEntity contract(@AuthenticationPrincipal SessionDTO session) {
		if (session == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		contractService.contract(session);
		return ResponseEntity.ok().build();
	}

}
