package com.br.itau.login.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.request.LoginRequest;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.model.response.MeResponseDTO;
import com.br.itau.login.service.LoginService;
import com.br.itau.login.service.MeService;
import com.br.itau.login.utils.UserAgentUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class LoginImpl implements Login {

	private final LoginService loginService;
	private final MeService meService;
	
	

	public LoginImpl(LoginService loginService,MeService meService) {
		this.loginService = loginService;
		this.meService = meService;
	}

	@Override
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		String userAgent = null;
		try {
			RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest req = (HttpServletRequest) RequestContextHolder.currentRequestAttributes()
						.resolveReference(RequestAttributes.REFERENCE_REQUEST);
				if (req != null) {
					userAgent = req.getHeader("User-Agent");
				}
			}
		} catch (Exception e) {
			// ignore
		}
		String channel = UserAgentUtils.mapToChannel(userAgent);
		return ResponseEntity.ok(loginService.login(loginRequest, channel));
	}

	@Override
	public ResponseEntity<MeResponseDTO> me(@AuthenticationPrincipal SessionDTO session) {
		if (session == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
						 MeResponseDTO dto = meService.getUserInfo(session.getSessionId(), session.getSessionId(), session.getUsername());
						 dto.setChannel(session.getChannel());
						 return ResponseEntity.ok(dto);
		
		
	}

}
