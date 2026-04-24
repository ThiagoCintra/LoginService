package com.br.itau.login.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationFailureEventListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationFailureEventListener.class);

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username = principal != null ? principal.toString() : "unknown";
        String ip = "unknown";
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object remoteAddr = attrs.getAttribute("REMOTE_ADDR", RequestAttributes.SCOPE_REQUEST);
            if (remoteAddr != null) {
                ip = remoteAddr.toString();
            }
        }
        if ("unknown".equals(ip)) {
            try {
                jakarta.servlet.http.HttpServletRequest req = (HttpServletRequest) RequestContextHolder
                        .currentRequestAttributes().resolveReference(RequestAttributes.REFERENCE_REQUEST);
                if (req != null) {
                    ip = com.br.itau.login.utils.IpUtils.getClientIp(req);
                }
            } catch (Exception e) {
                logger.warn("Could not extract client IP from request: {}", e.getMessage());
            }
        }
        logger.warn("Failed login attempt for user '{}' from IP {}", username, ip);
    }
}
