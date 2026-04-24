package com.br.itau.login.controller.contract;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.service.JwtService;
import com.br.itau.login.service.SessionService;

@RestController
public class ContractControllerIml implements ContractController {

	private final SessionService sessionService;
	private final JwtService jwtService;

	public ContractControllerIml(SessionService sessionService, JwtService jwtService) {
		this.sessionService = sessionService;
		this.jwtService = jwtService;
	}

	@Override
	// Allow the method to be invoked even when principal is null so we can return 500 as requested.
	// If principal is non-null enforce that contractService == false.
	@PreAuthorize("principal == null ? true : principal.contractService == false")
	public ResponseEntity contract(@AuthenticationPrincipal SessionDTO session) {
		if (session == null) {
			// per requirement: if session is null return 500
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		// mark that the client has contracted the service and persist the session
		session.setContractService(Boolean.TRUE);
		sessionService.save(session, jwtService.getExpirationMs());
		return ResponseEntity.ok().build();
	}

}
