package com.br.itau.login.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.br.itau.login.model.SessionDTO;

@Service
public class SessionServiceImpl implements SessionService {

	private final RedisTemplate<String, Object> redisTemplate;

	public SessionServiceImpl(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public SessionDTO find(String sessionId) {
		Object obj = redisTemplate.opsForValue().get(key(sessionId));
		if (obj instanceof SessionDTO) {
			return (SessionDTO) obj;
		}
		return null;
	}

	@Override
	public void delete(String sessionId) {
		redisTemplate.delete(key(sessionId));

	}

	private String key(String sessionId) {
		return "session:" + sessionId;
	}
	
	 public void save(SessionDTO session, long ttlMillis) {
	        redisTemplate.opsForValue().set(key(session.getSessionId()), session, Duration.ofMillis(ttlMillis));
	    }

}
