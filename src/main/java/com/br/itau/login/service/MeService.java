package com.br.itau.login.service;

import com.br.itau.login.model.response.MeResponseDTO;

public interface MeService {
    MeResponseDTO getUserInfo(String token, String sessionId, String username);
}
