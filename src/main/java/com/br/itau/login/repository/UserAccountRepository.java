package com.br.itau.login.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.itau.login.model.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}
