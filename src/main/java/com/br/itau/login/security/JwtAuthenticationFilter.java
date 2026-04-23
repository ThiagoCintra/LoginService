package com.br.itau.login.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.br.itau.login.model.SessionDTO;
import com.br.itau.login.service.JwtService;
import com.br.itau.login.service.SessionService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final SessionService sessionService;

	public JwtAuthenticationFilter(JwtService jwtService, SessionService sessionService) {
		this.jwtService = jwtService;
		this.sessionService = sessionService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Bearer token authentication is disabled for this deployment.
		// The JwtAuthenticationFilter logic (reading the Authorization header, parsing JWT, and
		// setting the SecurityContext) has been intentionally skipped/commented out.
		// If you need to re-enable JWT processing later, restore the original implementation.

		filterChain.doFilter(request, response);
	}

}
