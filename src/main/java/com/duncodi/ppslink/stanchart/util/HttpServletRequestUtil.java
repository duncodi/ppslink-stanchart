package com.duncodi.ppslink.stanchart.util;

import jakarta.servlet.http.HttpServletRequest;

public class HttpServletRequestUtil {

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();  // Fallback to direct IP
        }
        return ip.split(",")[0];  // Handle multiple IPs in X-Forwarded-For
    }

}
