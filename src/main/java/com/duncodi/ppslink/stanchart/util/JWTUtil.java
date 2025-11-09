package com.duncodi.ppslink.stanchart.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public class JWTUtil {

    public static String getTokenFromServletRequest(HttpServletRequest request){

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            return token;

        }

        return null;

    }

    public static String generateRandomJWTIdentifier(){
        return UUID.randomUUID().toString();
    }

    public static Integer generateIat() {
        // iat is the current time in seconds since epoch
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static Integer generateExpiry(Integer issuedAt, Double jwtExpiryMinutes) {

        // exp is iat + expiry minutes converted to seconds

        jwtExpiryMinutes = jwtExpiryMinutes==null?0.5:jwtExpiryMinutes;

        int currentTimeSeconds = issuedAt==null?((int) System.currentTimeMillis() / 1000):issuedAt;

        return (int) (currentTimeSeconds + (jwtExpiryMinutes * 60L));

    }

}
