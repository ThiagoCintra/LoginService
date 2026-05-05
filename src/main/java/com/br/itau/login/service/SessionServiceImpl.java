package com.br.itau.login.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.br.itau.login.model.SessionDTO;

@Service
public class SessionServiceImpl implements SessionService {

	private static final String SESSION_PREFIX = "session:";
	private static final String USER_SESSION_PREFIX = "user_session:";
	private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
	private static final String USER_REFRESH_TOKEN_PREFIX = "user_refresh_token:";

	private final RedisTemplate<String, Object> redisTemplate;

	public SessionServiceImpl(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public SessionDTO find(String sessionId) {
		Object obj = redisTemplate.opsForValue().get(sessionKey(sessionId));
		if (obj == null) {
			return null;
		}
		if (obj instanceof SessionDTO) {
			return (SessionDTO) obj;
		}
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
			Long userId = null;
			Object uid = map.get("userId");
			if (uid instanceof Number) {
				userId = ((Number) uid).longValue();
			} else if (uid != null) {
				userId = Long.valueOf(uid.toString());
			}
			Long escolaId = null;
			Object eid = map.get("escolaId");
			if (eid instanceof Number) {
				escolaId = ((Number) eid).longValue();
			} else if (eid != null) {
				escolaId = Long.valueOf(eid.toString());
			}
			return new SessionDTO(sid, username, contractService, symmetricKey, role, userId, escolaId);
		}
		return null;
	}

	@Override
	public void delete(String sessionId) {
		redisTemplate.delete(sessionKey(sessionId));
	}

	@Override
	public void save(SessionDTO session, long ttlMillis) {
		ValueOperations<String, Object> ops = redisTemplate.opsForValue();
		if (ops == null) {
			return;
		}
		ops.set(sessionKey(session.getSessionId()), session, Duration.ofMillis(ttlMillis));
	}

	@Override
	public void saveUserSession(Long userId, String sessionId, long ttlMillis) {
		ValueOperations<String, Object> ops = redisTemplate.opsForValue();
		if (ops == null) {
			return;
		}
		ops.set(userSessionKey(userId), sessionId, Duration.ofMillis(ttlMillis));
	}

	@Override
	public String findSessionIdByUserId(Long userId) {
		Object obj = redisTemplate.opsForValue().get(userSessionKey(userId));
		return obj != null ? obj.toString() : null;
	}

	@Override
	public void deleteUserSession(Long userId) {
		redisTemplate.delete(userSessionKey(userId));
	}

	@Override
	public void saveRefreshToken(String refreshToken, Long userId, long ttlMillis) {
		ValueOperations<String, Object> ops = redisTemplate.opsForValue();
		if (ops == null) {
			return;
		}
		ops.set(refreshTokenKey(refreshToken), userId.toString(), Duration.ofMillis(ttlMillis));
	}

	@Override
	public Long findUserIdByRefreshToken(String refreshToken) {
		Object obj = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
		if (obj == null) {
			return null;
		}
		try {
			return Long.valueOf(obj.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void deleteRefreshToken(String refreshToken) {
		redisTemplate.delete(refreshTokenKey(refreshToken));
	}

	private String sessionKey(String sessionId) {
		return SESSION_PREFIX + sessionId;
	}

	private String userSessionKey(Long userId) {
		return USER_SESSION_PREFIX + userId;
	}

	private String refreshTokenKey(String refreshToken) {
		return REFRESH_TOKEN_PREFIX + refreshToken;
	}

	private String userRefreshTokenKey(Long userId) {
		return USER_REFRESH_TOKEN_PREFIX + userId;
	}

	@Override
	public void saveUserRefreshToken(Long userId, String refreshToken, long ttlMillis) {
		ValueOperations<String, Object> ops = redisTemplate.opsForValue();
		if (ops == null) {
			return;
		}
		ops.set(userRefreshTokenKey(userId), refreshToken, Duration.ofMillis(ttlMillis));
	}

	@Override
	public String findRefreshTokenByUserId(Long userId) {
		Object obj = redisTemplate.opsForValue().get(userRefreshTokenKey(userId));
		return obj != null ? obj.toString() : null;
	}

	@Override
	public void deleteUserRefreshToken(Long userId) {
		redisTemplate.delete(userRefreshTokenKey(userId));
	}
}
