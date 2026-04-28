package com.br.itau.login.service;

import com.br.itau.login.model.SessionDTO;

public interface SessionService {
	SessionDTO find(String sessionId);
	void delete(String sessionId);
	void save(SessionDTO session, long ttlMillis);
	long getTtlMillis(String sessionId);
}
