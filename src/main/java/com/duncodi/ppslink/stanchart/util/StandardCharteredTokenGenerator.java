package com.duncodi.ppslink.stanchart.util;

import com.duncodi.ppslink.stanchart.enums.YesNo;
import com.duncodi.ppslink.stanchart.model.StraightToBankConfig;
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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StandardCharteredTokenGenerator {

    @Value("${scbkeys.private-key-path}")
    private String privateKeyPath;

    public String generateScbJwtToken(StraightToBankConfig config) {

        try {

            PrivateKey privateKey = loadScbPrivateKey();

            Map<String, Object> claims = prepareClaims(config);

            // Generate SCB JWT token
            return Jwts.builder()
                    .setHeaderParam("alg", "RS256")
                    .setHeaderParam("typ", "JWT")
                    .setClaims(claims)
                    .signWith(SignatureAlgorithm.RS256, privateKey)
                    .compact();

        } catch (Exception e) {
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

        String webhookUrl = config.getWebhookUrl()==null?"":config.getWebhookUrl();
        boolean webhookEnabled = YesNo.YES.equals(config.getEnableWebhook());

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("webHookUrl", webhookUrl);
        payloadMap.put("enableWebHook", webhookEnabled);

        String activationJson = config.getActivationKey()!=null?EncryptDecrypt.decrypt(config.getActivationKey()):"";

        activationJson = activationJson==null?"":activationJson;

        String content = activationJson.replaceAll(".*\"content\":\"([^\"]+)\".*", "$1");
        String key = activationJson.replaceAll(".*\"key\":\"([^\"]+)\".*", "$1");

        log.info("activation key json>>>>>>>>>>"+activationJson);
        log.info("content>>>>>>>>>>"+content);
        log.info("key>>>>>>>>>>"+key);

        Map<String, Object> activationKeyMap = new HashMap<>();

        activationKeyMap.put("content", content);
        activationKeyMap.put("key", key);

        payloadMap.put("activationKey", activationKeyMap);
        claims.put("payload", payloadMap);

        return claims;

    }

    private PrivateKey loadScbPrivateKey() throws Exception {

        log.info("privateKeyPath>>>>>>>>"+privateKeyPath);

        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}