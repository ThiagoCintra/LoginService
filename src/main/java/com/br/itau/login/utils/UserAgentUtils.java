package com.br.itau.login.utils;

public class UserAgentUtils {

    public static String mapToChannel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "API";
        }
        String ua = userAgent.toLowerCase();

        // API clients
        if (ua.contains("postman") || ua.contains("curl") || ua.contains("insomnia")) {
            return "API";
        }

        // Mobile indicators
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
            return "MOBILE";
        }

        // Otherwise assume web desktop
        return "WEB";
    }
}
