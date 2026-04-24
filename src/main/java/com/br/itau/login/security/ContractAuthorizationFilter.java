package com.br.itau.login.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.br.itau.login.model.SessionDTO;

@Component
public class ContractAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // only filter the contract endpoint
        return !"/api/v1/contract".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (principal == null || !(principal instanceof SessionDTO)) {
            // per requirement, when session is null return 500
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Session not found");
            return;
        }

        SessionDTO session = (SessionDTO) principal;
        if (Boolean.TRUE.equals(session.getContractService())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service already contracted");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
