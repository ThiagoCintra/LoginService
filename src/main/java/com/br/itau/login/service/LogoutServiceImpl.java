package com.br.itau.login.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.br.itau.login.model.SessionDTO;

import io.jsonwebtoken.Claims;

@Service
public class LogoutServiceImpl implements LogoutService {

    private final JwtService jwtService;
    private final SessionService sessionService;

    public LogoutServiceImpl(JwtService jwtService, SessionService sessionService) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Override
    public void logout(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            return;
        }

        Claims claims = jwtService.getClaims(token);
        String sessionId = claims.get("sessionId", String.class);

        if (sessionId != null) {
            SessionDTO session = sessionService.find(sessionId);
            if (session != null && session.getUserId() != null) {
                Long userId = session.getUserId();
                // Remove refresh token and its reverse lookup
                String refreshToken = sessionService.findRefreshTokenByUserId(userId);
                if (refreshToken != null) {
                    sessionService.deleteRefreshToken(refreshToken);
                }
                sessionService.deleteUserRefreshToken(userId);
                sessionService.deleteUserSession(userId);
            }
            sessionService.delete(sessionId);
        }

        SecurityContextHolder.clearContext();
    }
}
