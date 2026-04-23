package com.br.itau.login.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.br.itau.login.model.entity.UserAccount;
import com.br.itau.login.model.enums.Role;
import com.br.itau.login.repository.UserAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String username = "Thiago";
        userRepository.findByUsername(username).ifPresentOrElse(u -> {
        }, () -> {
            UserAccount user = new UserAccount();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("231299"));
            user.setNomeCompleto("Thiago");
            user.setEmail(null);
            user.setContractService(Boolean.FALSE);
            user.setRole(Role.USER);
            userRepository.save(user);
            System.out.println("Inserted initial user 'Thiago'");
        });
    }
}
