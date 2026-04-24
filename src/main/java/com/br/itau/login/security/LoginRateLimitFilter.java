package com.br.itau.login.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.br.itau.login.service.LoginRateLimiter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;
    private final Logger logger = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !"/api/v1/auth/login".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = com.br.itau.login.utils.IpUtils.getClientIp(request);
        // expose IP to RequestContextHolder so listeners can read it (used by AuthenticationFailureEventListener)
        RequestContextHolder.currentRequestAttributes().setAttribute("REMOTE_ADDR", ip, RequestAttributes.SCOPE_REQUEST);
        if (!rateLimiter.tryConsume(ip)) {
            logger.warn("Rate limit exceeded for IP {} on /auth/login", ip);
            response.sendError(429, "Too Many Requests");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
