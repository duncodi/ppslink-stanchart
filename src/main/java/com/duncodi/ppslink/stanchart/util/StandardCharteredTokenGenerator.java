package com.duncodi.ppslink.stanchart.util;

import com.duncodi.ppslink.stanchart.dto.StraightToBankConfigDto;
import com.duncodi.ppslink.stanchart.enums.YesNo;
import com.duncodi.ppslink.stanchart.model.StraightToBankConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StandardCharteredTokenGenerator {

    @Value("${scb.jwt.private-key.path}")
    private String privateKeyPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateScbJwtToken(StraightToBankConfig config) {
        try {
            log.info("Starting JWT token generation...");

            PrivateKey privateKey = loadScbPrivateKey();
            Map<String, Object> claims = prepareClaims(config);

            log.info("Creating JWT with RSA256 signature...");

            String token = Jwts.builder()
                    .setHeaderParam("alg", "RS256")
                    .setHeaderParam("typ", "JWT")
                    .setClaims(claims)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            log.info("✅ JWT token generated successfully. Length: {}", token.length());
            return token;

        } catch (Exception e) {
            log.error("❌ Failed to generate Standard Chartered JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Standard Chartered JWT token", e);
        }
    }

    public String generateScbJwtToken(StraightToBankConfigDto config) {
        try {
            log.info("Starting JWT token generation...");

            PrivateKey privateKey = loadScbPrivateKey();
            Map<String, Object> claims = prepareClaims(config);

            log.info("Creating JWT with RSA256 signature...");

            String token = Jwts.builder()
                    .setHeaderParam("alg", "RS256")
                    .setHeaderParam("typ", "JWT")
                    .setClaims(claims)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            log.info("✅ JWT token generated successfully. Length: {}", token.length());
            return token;

        } catch (Exception e) {
            log.error("❌ Failed to generate Standard Chartered JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Standard Chartered JWT token", e);
        }
    }

    private PrivateKey loadScbPrivateKey() throws Exception {
        log.info("Loading private key from: {}", privateKeyPath);

        if (!Files.exists(Paths.get(privateKeyPath))) {
            throw new RuntimeException("Private key file not found: " + privateKeyPath);
        }

        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)));

        log.info("Private key file size: {} bytes", privateKeyContent.length());

        // Check if the file contains multiple keys or just a clean PEM
        if (privateKeyContent.contains("Bag Attributes")) {
            log.warn("Private key file contains multiple keys or PKCS12 attributes. Extracting first private key...");
            privateKeyContent = extractFirstPrivateKey(privateKeyContent);
        } else {
            log.info("Private key file appears to be clean PEM format");
        }

        // Clean the PEM content
        privateKeyContent = cleanPemContent(privateKeyContent);

        log.info("Cleaned key content length: {}", privateKeyContent.length());
        log.debug("First 50 chars of cleaned key: {}", privateKeyContent.substring(0, Math.min(50, privateKeyContent.length())));

        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            log.info("✅ Private key loaded successfully. Algorithm: {}, Format: {}",
                    privateKey.getAlgorithm(), privateKey.getFormat());
            return privateKey;

        } catch (IllegalArgumentException e) {
            log.error("❌ Base64 decoding failed: {}", e.getMessage());
            log.error("Key content that failed (first 500 chars): {}",
                    privateKeyContent.substring(0, Math.min(500, privateKeyContent.length())));

            // Try alternative approach - maybe it's RSA private key format
            try {
                log.info("🔄 Trying alternative PKCS1 to PKCS8 conversion...");
                return loadPrivateKeyAlternative(privateKeyContent);
            } catch (Exception altException) {
                log.error("❌ Alternative approach also failed: {}", altException.getMessage());
                throw new RuntimeException("Failed to decode private key - invalid Base64 format. Ensure the key file contains a single clean PEM private key.", e);
            }
        } catch (Exception e) {
            log.error("❌ Failed to load private key: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String extractFirstPrivateKey(String privateKeyContent) {
        try {
            // Split by private key boundaries and take the first one
            String[] keySections = privateKeyContent.split("-----BEGIN PRIVATE KEY-----");
            if (keySections.length > 1) {
                String firstKey = keySections[1].split("-----END PRIVATE KEY-----")[0];
                String cleanKey = "-----BEGIN PRIVATE KEY-----" + firstKey + "-----END PRIVATE KEY-----";
                log.info("✅ Extracted first private key from multi-key file");
                return cleanKey;
            } else {
                throw new RuntimeException("No private key found in file");
            }
        } catch (Exception e) {
            log.error("Failed to extract private key: {}", e.getMessage());
            throw new RuntimeException("Failed to extract private key from multi-key file", e);
        }
    }

    private String cleanPemContent(String privateKeyContent) {
        // Remove all PEM headers/footers and whitespace
        return privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replace("-----END ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace, newlines, etc.
    }

    private PrivateKey loadPrivateKeyAlternative(String privateKeyContent) throws Exception {
        // If it's RSA private key (PKCS1), we need to convert to PKCS8
        if (privateKeyContent.contains("BEGIN RSA PRIVATE KEY")) {
            log.info("🔧 Converting RSA private key (PKCS1) to PKCS8 format...");

            // For PKCS1 to PKCS8 conversion, we'd need BouncyCastle
            // For now, let's try to handle it as PKCS8 if possible
            throw new RuntimeException("RSA private key (PKCS1) format detected. Please use PKCS8 format or add BouncyCastle dependency.");
        }

        // Try with different Base64 decoding options
        try {
            // Try URL-safe decoder
            byte[] keyBytes = Base64.getUrlDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (IllegalArgumentException e) {
            // Try MIME decoder as last resort
            byte[] keyBytes = Base64.getMimeDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    private Map<String, Object> prepareClaims(StraightToBankConfig config) {
        String jti = JWTUtil.generateRandomJWTIdentifier();
        Integer iat = JWTUtil.generateIat();
        Integer exp = JWTUtil.generateExpiry(iat, config.getJwtExpiryMinutes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", config.getJwtIssuer());
        claims.put("sub", config.getJwtIssuer());
        claims.put("jti", jti);
        claims.put("aud", config.getJwtAudience());
        claims.put("iat", iat);
        claims.put("exp", exp);
        claims.put("nbf", iat);

        log.info("🔹 JWT Claims:");
        log.info("🔹   Issuer (iss): {}", config.getJwtIssuer());
        log.info("🔹   Subject (sub): {}", config.getJwtIssuer());
        log.info("🔹   Audience (aud): {}", config.getJwtAudience());
        log.info("🔹   JWT ID (jti): {}", jti);
        log.info("🔹   Issued At (iat): {}", iat);
        log.info("🔹   Expiry (exp): {}", exp);
        log.info("🔹   Token expiry minutes: {}", config.getJwtExpiryMinutes());

        String webhookUrl = config.getWebhookUrl() == null ? "" : config.getWebhookUrl();
        boolean webhookEnabled = YesNo.YES.equals(config.getEnableWebhook());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("webHookUrl", webhookUrl);
        payloadMap.put("enableWebHook", webhookEnabled);

        // FIXED: Use the activation key content directly as string (not nested object)
        String activationContent = extractActivationContentSimple(config.getActivationKey());
        payloadMap.put("activationKey", activationContent);

        claims.put("payload", payloadMap);

        // Log final JWT structure for debugging
        try {
            String claimsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
            log.info("🎯 FINAL JWT CLAIMS STRUCTURE:\n{}", claimsJson);
        } catch (Exception e) {
            log.warn("Could not serialize claims for debugging");
        }

        return claims;
    }

    private Map<String, Object> prepareClaims(StraightToBankConfigDto config) {
        String jti = JWTUtil.generateRandomJWTIdentifier();
        Integer iat = JWTUtil.generateIat();
        Integer exp = JWTUtil.generateExpiry(iat, config.getJwtExpiryMinutes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", config.getJwtIssuer());
        claims.put("sub", config.getJwtIssuer());
        claims.put("jti", jti);
        claims.put("aud", config.getJwtAudience());
        claims.put("iat", iat);
        claims.put("exp", exp);
        claims.put("nbf", iat);

        log.info("🔹 JWT Claims:");
        log.info("🔹   Issuer (iss): {}", config.getJwtIssuer());
        log.info("🔹   Subject (sub): {}", config.getJwtIssuer());
        log.info("🔹   Audience (aud): {}", config.getJwtAudience());
        log.info("🔹   JWT ID (jti): {}", jti);
        log.info("🔹   Issued At (iat): {}", iat);
        log.info("🔹   Expiry (exp): {}", exp);
        log.info("🔹   Token expiry minutes: {}", config.getJwtExpiryMinutes());

        String webhookUrl = config.getWebhookUrl() == null ? "" : config.getWebhookUrl();
        boolean webhookEnabled = YesNo.YES.equals(config.getEnableWebhook());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("webHookUrl", webhookUrl);
        payloadMap.put("enableWebHook", webhookEnabled);

        // FIXED: Use the activation key content directly as string (not nested object)
        String activationContent = extractActivationContentSimple(config.getActivationKey());
        payloadMap.put("activationKey", activationContent);

        claims.put("payload", payloadMap);

        // Log final JWT structure for debugging
        try {
            String claimsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
            log.info("🎯 FINAL JWT CLAIMS STRUCTURE:\n{}", claimsJson);
        } catch (Exception e) {
            log.warn("Could not serialize claims for debugging");
        }

        return claims;
    }

    /**
     * SIMPLIFIED VERSION: Extract only the content from activation key
     * SCB expects the activation key as a simple string, not an object with key/value
     */
    private String extractActivationContentSimple(String activationKey) {
        if (activationKey == null || activationKey.trim().isEmpty()) {
            log.error("❌ Activation key is null or empty");
            throw new RuntimeException("Activation key is required for SCB authentication");
        }

        try {
            log.info("🔄 Parsing activation key content...");

            String trimmedKey = activationKey.trim();

            // If it's a JSON object, extract the content field
            if (trimmedKey.startsWith("{") && trimmedKey.endsWith("}")) {
                JsonNode rootNode = objectMapper.readTree(activationKey);

                if (rootNode.has("content")) {
                    String content = rootNode.get("content").asText();
                    if (content != null && !content.trim().isEmpty()) {
                        log.info("✅ Activation key content extracted, length: {}", content.length());
                        return content;
                    } else {
                        throw new RuntimeException("Activation key content is empty");
                    }
                } else {
                    throw new RuntimeException("No 'content' field found in activation key JSON");
                }
            } else {
                // If it's already the raw content, use it directly
                log.info("✅ Using activation key as raw content, length: {}", trimmedKey.length());
                return trimmedKey;
            }

        } catch (Exception e) {
            log.error("❌ Failed to parse activation key: {}", e.getMessage());
            throw new RuntimeException("Invalid activation key format: " + e.getMessage(), e);
        }
    }
}