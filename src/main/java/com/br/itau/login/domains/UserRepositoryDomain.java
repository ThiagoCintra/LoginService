package com.br.itau.login.domains;

import java.util.Optional;

import com.br.itau.login.model.entity.UserAccount;

public interface UserRepositoryDomain {
	Optional<UserAccount> findByUsername(String username);

	Optional<UserAccount> findById(Long id);

	UserAccount save(UserAccount user);
}
