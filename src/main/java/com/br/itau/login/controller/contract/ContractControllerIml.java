package com.br.itau.login.controller.contract;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.service.SessionService;

@RestController
public class ContractControllerIml implements ContractController {

	private final SessionService sessionService;

	public ContractControllerIml(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@Override
	public ResponseEntity contract(@AuthenticationPrincipal SessionDTO session) {
		if (session == null) {
			// per requirement: if session is null return 500
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		return ResponseEntity.ok().build();
	}

}
