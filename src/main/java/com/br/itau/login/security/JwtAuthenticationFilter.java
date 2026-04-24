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
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			try {
				if (jwtService.isTokenValid(token)) {
					Claims claims = jwtService.getClaims(token);
					String username = claims.getSubject();
					String sessionId = claims.get("sessionId", String.class);
					Boolean contractService = claims.get("contractService", Boolean.class);
					SessionDTO session = sessionService.find(sessionId);
					if (Objects.isNull(session)) {
						SecurityContextHolder.clearContext();
					} else {
						List<GrantedAuthority> authorities = new ArrayList<>();
						if (Objects.nonNull(session.getRole())) {
							authorities.add(new SimpleGrantedAuthority("ROLE_" + session.getRole()));
						}
						UserDetails userDetails = User.withUsername(username).password("").authorities(authorities)
								.build();
						UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(session,
								null, authorities);
						auth.setDetails(userDetails);

						SecurityContextHolder.getContext().setAuthentication(auth);
					}
				}
			} catch (Exception ex) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
