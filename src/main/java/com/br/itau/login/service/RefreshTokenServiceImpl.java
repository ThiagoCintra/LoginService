package com.br.itau.login.service;

import org.springframework.stereotype.Service;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.response.AuthResponse;
import com.br.itau.login.utils.SessionUtils;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final SessionService sessionService;
    private final UserRepositoryDomain userRepositoryPort;
    private final SessionUtils sessionUtils;

    public RefreshTokenServiceImpl(SessionService sessionService,
                                   UserRepositoryDomain userRepositoryPort,
                                   SessionUtils sessionUtils) {
        this.sessionService = sessionService;
        this.userRepositoryPort = userRepositoryPort;
        this.sessionUtils = sessionUtils;
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token não informado");
        }

        Long userId = sessionService.findUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            throw new IllegalArgumentException("Refresh token inválido ou expirado");
        }

        UserAccount user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String roleName = user.getRole() != null ? user.getRole().name() : null;

        // Invalidate old session
        String oldSessionId = sessionService.findSessionIdByUserId(userId);
        if (oldSessionId != null) {
            sessionService.delete(oldSessionId);
        }

        // Create new session
        String newSessionId = sessionUtils.generateSessionId();
        String symmetricKey = sessionUtils.generateSymmetricKey();

        SessionDTO newSession = new SessionDTO(newSessionId, user.getUsername(), user.getContractService(),
                symmetricKey, roleName, userId, user.getEscolaId());

        sessionUtils.saveSession(newSession);
        String newAccessToken = sessionUtils.createToken(user.getUsername(), newSessionId, roleName,
                user.getContractService(), user.getEscolaId());

        // Keep the same refresh token (no rotation)
        return new AuthResponse(newAccessToken, refreshToken);
    }
}
