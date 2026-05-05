package com.br.itau.login.service;

import com.br.itau.login.model.response.AuthResponse;

public interface RefreshTokenService {
    AuthResponse refresh(String refreshToken);
}
