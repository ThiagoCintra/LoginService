package com.br.itau.login.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
		if (obj == null) {
			return null;
		}
		// If Redis deserialized directly to SessionDTO (unlikely with JSON serializer)
		if (obj instanceof SessionDTO) {
			return (SessionDTO) obj;
		}
		// If value was deserialized into a Map (GenericJackson2JsonRedisSerializer) convert to SessionDTO
		if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) obj;
			String sid = map.get("sessionId") != null ? map.get("sessionId").toString() : null;
			String username = map.get("username") != null ? map.get("username").toString() : null;
			Boolean contractService = null;
			Object cs = map.get("contractService");
			if (cs instanceof Boolean) {
				contractService = (Boolean) cs;
			} else if (cs != null) {
				contractService = Boolean.valueOf(cs.toString());
			}
			String symmetricKey = map.get("symmetricKey") != null ? map.get("symmetricKey").toString() : null;
			String role = map.get("role") != null ? map.get("role").toString() : null;
			return new SessionDTO(sid, username, contractService, symmetricKey, role);
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
			// opsForValue() can be null in some test setups (mocked RedisTemplate).
			// Guard against NPE: if ValueOperations is null, do nothing.
			ValueOperations<String, Object> ops = redisTemplate.opsForValue();
			if (ops == null) {
				return;
			}
			ops.set(key(session.getSessionId()), session, Duration.ofMillis(ttlMillis));
		}

}
