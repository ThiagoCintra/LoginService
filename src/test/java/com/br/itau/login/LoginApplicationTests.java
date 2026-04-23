package com.br.itau.login;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.br.itau.login.config.TestRedisConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class LoginApplicationTests {

	@Test
	void contextLoads() {
	}

}
