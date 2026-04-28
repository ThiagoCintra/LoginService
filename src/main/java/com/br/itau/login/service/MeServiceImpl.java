package com.br.itau.login.service;

import org.springframework.stereotype.Service;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.response.MeResponseDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

import io.jsonwebtoken.Claims;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
@Service
public class MeServiceImpl implements MeService {

    private static final Logger logger = LoggerFactory.getLogger(MeServiceImpl.class);

    private final UserRepositoryDomain userRepositoryPort;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final UserDetailsService userDetailsService;

    public MeServiceImpl(UserRepositoryDomain userRepositoryPort,
                         JwtService jwtService,
                         SessionService sessionService,
                         UserDetailsService userDetailsService) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public MeResponseDTO getUserInfo(String token, String sessionId, String username) {
        logger.debug("Getting user info for username: {}", username);

        // 1. Validar sessão
        SessionDTO session = sessionService.find(sessionId);
        if (session == null) {
            logger.warn("Session not found for sessionId: {}", sessionId);
            throw new RuntimeException("Session not found");
        }

        // 2. Reuse existing UserDetailsService to validate/load user
        userDetailsService.loadUserByUsername(username);

        // 3. Buscar usuário no banco (para fields like id)
        UserAccount user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        // 4. Channel comes from session (no hard-code)
        String channel = session.getChannel();

        // 4. Construir resposta
        MeResponseDTO response = new MeResponseDTO();
        response.setId(user.getId());
        response.setSessionId(session.getSessionId());
        response.setUsername(session.getUsername());
        response.setContractService(session.getContractService());
        response.setRole(session.getRole());
        response.setChannel(channel);

        logger.info("User info retrieved successfully for: {}", username);
        return response;
    }

    // channel is read from session in Redis; no token extraction here
}
