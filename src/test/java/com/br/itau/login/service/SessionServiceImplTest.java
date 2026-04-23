package com.br.itau.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.br.itau.login.model.SessionDTO;

class SessionServiceImplTest {

	private RedisTemplate<String, Object> redisTemplate;
	private ValueOperations<String, Object> valueOps;
	private SessionServiceImpl sessionService;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
		redisTemplate = mock(RedisTemplate.class);
		valueOps = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		sessionService = new SessionServiceImpl(redisTemplate);
	}

	@Test
	void save_shouldStoreSessionWithTtl() {
		SessionDTO session = new SessionDTO("sid-1", "user1", Boolean.FALSE, "key", "USER");
		sessionService.save(session, 60_000L);
		verify(valueOps).set(eq("session:sid-1"), eq(session), eq(Duration.ofMillis(60_000L)));
	}

	@Test
	void find_shouldReturnSessionWhenPresent() {
		SessionDTO expected = new SessionDTO("sid-1", "user1", Boolean.TRUE, "key", "USER");
		when(valueOps.get("session:sid-1")).thenReturn(expected);

		SessionDTO result = sessionService.find("sid-1");

		assertThat(result).isEqualTo(expected);
	}

	@Test
	void find_shouldReturnNullWhenNotPresent() {
		when(valueOps.get("session:missing")).thenReturn(null);

		SessionDTO result = sessionService.find("missing");

		assertThat(result).isNull();
	}

	@Test
	void delete_shouldRemoveSessionKey() {
		sessionService.delete("sid-1");
		verify(redisTemplate).delete("session:sid-1");
	}
}
