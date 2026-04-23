package com.br.itau.login.adpters;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.br.itau.login.domains.UserRepositoryDomain;
import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.repository.UserAccountRepository;

@Component
public class UserRepositoryAdapter implements UserRepositoryDomain {

	private final UserAccountRepository userAccountRepository;

	@Autowired
	public UserRepositoryAdapter(UserAccountRepository UserAccountRepository) {
		this.userAccountRepository = UserAccountRepository;
	}

	@Override
	public Optional<UserAccount> findByUsername(String username) {
		return userAccountRepository.findByUsername(username);
	}

	@Override
	public Optional<UserAccount> findById(Long id) {
		return userAccountRepository.findById(id);
	}

	@Override
	public UserAccount save(UserAccount user) {
		return userAccountRepository.save(user);
	}

}
