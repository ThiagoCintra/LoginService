package com.br.itau.login.service;

import org.springframework.stereotype.Service;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.response.MeResponseDTO;

import io.jsonwebtoken.Claims;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
@Service
public class MeServiceImpl implements MeService {

    private static final Logger logger = LoggerFactory.getLogger(MeServiceImpl.class);

    private final UserRepositoryDomain userRepositoryPort;
    private final JwtService jwtService;
    private final SessionService sessionService;

    public MeServiceImpl(UserRepositoryDomain userRepositoryPort, 
                         JwtService jwtService, 
                         SessionService sessionService) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
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

        // 2. Buscar usuário no banco
        UserAccount user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        // 3. Extrair channel do token
        String channel = extractChannelFromToken(token);

        // 4. Construir resposta
        MeResponseDTO response = new MeResponseDTO();
        response.setId(user.getId());
        response.setSessionId(session.getSessionId());
        response.setUsername(session.getUsername());
        response.setContractService(session.getContractService());
        response.setRole(session.getRole());
        response.setChannel(channel);
        response.setEscolaId(session.getEscolaId());

        logger.info("User info retrieved successfully for: {}", username);
        return response;
    }

    private String extractChannelFromToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.debug("No token provided for channel extraction");
            return null;
        }

        try {
            Claims claims = jwtService.getClaims(token);
            String channel = claims.get("channel", String.class);
            logger.debug("Channel extracted from token: {}", channel);
            return channel;
        } catch (Exception e) {
            logger.warn("Failed to extract channel from token: {}", e.getMessage());
            return null;
        }
    }
}
