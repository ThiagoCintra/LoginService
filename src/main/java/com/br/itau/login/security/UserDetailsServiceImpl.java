package com.br.itau.login.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.repository.UserAccountRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	public UserDetailsServiceImpl(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount account = userAccountRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		String role = account.getRole() != null ? account.getRole().name() : "USER";
		return new User(account.getUsername(), account.getPassword(),
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}
}
