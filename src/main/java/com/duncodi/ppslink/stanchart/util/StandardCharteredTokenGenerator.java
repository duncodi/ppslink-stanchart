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

    @Value("${scbkeys.private-key-path}")
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

            log.info("JWT token generated successfully");
            return token;

        } catch (Exception e) {
            log.error("Failed to generate Standard Chartered JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Standard Chartered JWT token", e);
        }
    }

    private Map<String, Object> prepareClaims(StraightToBankConfig config) {
        String jti = JWTUtil.generateRandomJWTIdentifier();
        Integer iat = JWTUtil.generateIat();
        Integer exp = JWTUtil.generateExpiry(iat, config.getJwtExpiryMinutes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", config.getJwtIssuer());
        claims.put("jti", jti);
        claims.put("aud", config.getJwtAudience());
        claims.put("iat", iat);
        claims.put("exp", exp);

        log.info("🔹 JWT Identifier (jti): {}", jti);
        log.info("🔹 Issued At (iat): {} (timestamp)", iat);
        log.info("🔹 Expiry (exp): {} (timestamp)", exp);
        log.info("🔹 Issuer (iss): {}", config.getJwtIssuer());
        log.info("🔹 Audience (aud): {}", config.getJwtAudience());
        log.info("🔹 Token expiry minutes from config: {}", config.getJwtExpiryMinutes());

        String webhookUrl = config.getWebhookUrl() == null ? "" : config.getWebhookUrl();
        boolean webhookEnabled = YesNo.YES.equals(config.getEnableWebhook());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("webHookUrl", webhookUrl);
        payloadMap.put("enableWebHook", webhookEnabled);

        // Parse activation key properly
        Map<String, Object> activationKeyMap = parseActivationKey(config.getActivationKey());
        payloadMap.put("activationKey", activationKeyMap);

        claims.put("payload", payloadMap);

        return claims;
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

            log.info("JWT token generated successfully");
            return token;

        } catch (Exception e) {
            log.error("Failed to generate Standard Chartered JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Standard Chartered JWT token", e);
        }
    }

    private Map<String, Object> prepareClaims(StraightToBankConfigDto config) {
        String jti = JWTUtil.generateRandomJWTIdentifier();
        Integer iat = JWTUtil.generateIat();
        Integer exp = JWTUtil.generateExpiry(iat, config.getJwtExpiryMinutes());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", config.getJwtIssuer());
        claims.put("jti", jti);
        claims.put("aud", config.getJwtAudience());
        claims.put("iat", iat);
        claims.put("exp", exp);

        log.info("🔹 JWT Identifier (jti): {}", jti);
        log.info("🔹 Issued At (iat): {} (timestamp)", iat);
        log.info("🔹 Expiry (exp): {} (timestamp)", exp);
        log.info("🔹 Issuer (iss): {}", config.getJwtIssuer());
        log.info("🔹 Audience (aud): {}", config.getJwtAudience());
        log.info("🔹 Token expiry minutes from config: {}", config.getJwtExpiryMinutes());

        String webhookUrl = config.getWebhookUrl() == null ? "" : config.getWebhookUrl();
        boolean webhookEnabled = YesNo.YES.equals(config.getEnableWebhook());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("webHookUrl", webhookUrl);
        payloadMap.put("enableWebHook", webhookEnabled);

        // Parse activation key properly
        Map<String, Object> activationKeyMap = parseActivationKey(config.getActivationKey());
        payloadMap.put("activationKey", activationKeyMap);

        claims.put("payload", payloadMap);

        return claims;
    }

    private Map<String, Object> parseActivationKey(String activationKey) {
        Map<String, Object> activationKeyMap = new HashMap<>();

        if (activationKey == null || activationKey.trim().isEmpty()) {
            log.warn("Activation key is null or empty");
            activationKeyMap.put("content", "");
            activationKeyMap.put("key", "");
            return activationKeyMap;
        }

        try {
            String decryptedActivationKey = EncryptDecrypt.decrypt(activationKey);
            log.info("Decrypted activation key JSON length: {}",
                    decryptedActivationKey != null ? decryptedActivationKey.length() : 0);

            if (decryptedActivationKey == null || decryptedActivationKey.trim().isEmpty()) {
                log.warn("Decrypted activation key is empty");
                activationKeyMap.put("content", "");
                activationKeyMap.put("key", "");
                return activationKeyMap;
            }

            // Parse JSON properly using Jackson
            JsonNode rootNode = objectMapper.readTree(decryptedActivationKey);

            String content = rootNode.has("content") ? rootNode.get("content").asText() : "";
            String key = rootNode.has("key") ? rootNode.get("key").asText() : "";

            boolean contentExtracted = content != null && !content.isEmpty();
            boolean keyExtracted = key != null && !key.isEmpty();

            log.info("Content extracted: {}", contentExtracted);
            log.info("Key extracted: {}", keyExtracted);

            if (!contentExtracted || !keyExtracted) {
                log.warn("⚠️ Activation key may be incomplete. Content: {}, Key: {}", contentExtracted, keyExtracted);
            }

            activationKeyMap.put("content", content);
            activationKeyMap.put("key", key);

        } catch (Exception e) {
            log.error("Failed to parse activation key: {}", e.getMessage());
            log.error("Activation key content that failed: {}", activationKey);
            activationKeyMap.put("content", "");
            activationKeyMap.put("key", "");
        }

        return activationKeyMap;
    }

    private PrivateKey loadScbPrivateKey() throws Exception {
        log.info("Loading private key from: {}", privateKeyPath);

        if (!Files.exists(Paths.get(privateKeyPath))) {
            throw new RuntimeException("Private key file not found: " + privateKeyPath);
        }

        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        log.info("Private key content length: {}", privateKeyContent.length());

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}