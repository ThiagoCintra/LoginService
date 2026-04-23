package com.br.itau.login.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
@Profile("test")
public class TestRedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
		@SuppressWarnings("unchecked")
		RedisTemplate<String, Object> mock = Mockito.mock(RedisTemplate.class);
		return mock;
	}
}
