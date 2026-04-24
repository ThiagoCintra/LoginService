package com.br.itau.login.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility to extract client IP considering proxies (X-Forwarded-For, X-Real-IP).
 */
public final class IpUtils {

    private IpUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may contain a list of IPs: client, proxy1, proxy2
            String[] parts = xff.split(",");
            for (String part : parts) {
                String ip = part.trim();
                if (!ip.isEmpty()) return ip;
            }
        }
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) {
            return xr.trim();
        }
        return request.getRemoteAddr();
    }
}
