package com.br.itau.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;

@Service
public class ContractServiceImpl implements ContractService {

	private static final Logger logger = LoggerFactory.getLogger(ContractServiceImpl.class);

	private final UserRepositoryDomain userRepositoryPort;
	private final SessionService sessionService;
	private final JwtService jwtService;

	public ContractServiceImpl(UserRepositoryDomain userRepositoryPort, SessionService sessionService, JwtService jwtService) {
		this.userRepositoryPort = userRepositoryPort;
		this.sessionService = sessionService;
		this.jwtService = jwtService;
	}

	@Override
	@Transactional
	public void contract(SessionDTO session) {
		if (session == null) {
			throw new RuntimeException("Session is required");
		}

		UserAccount user = userRepositoryPort.findByUsername(session.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found: " + session.getUsername()));

		if (Boolean.TRUE.equals(user.getContractService())) {
			throw new RuntimeException("User already has contract");
		}

		// 1. Update DB
		user.setContractService(Boolean.TRUE);
		userRepositoryPort.save(user);

		// 2. Update Redis session to reflect contractService
		SessionDTO current = sessionService.find(session.getSessionId());
		if (current == null) {
			current = session;
		}
		current.setContractService(Boolean.TRUE);

		long ttl = sessionService.getTtlMillis(current.getSessionId());
		if (ttl <= 0) {
			ttl = jwtService.getExpirationMs();
		}
		sessionService.save(current, ttl);

		logger.info("Contract activated for user: {}", user.getUsername());
	}
}
