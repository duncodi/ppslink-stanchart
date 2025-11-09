package com.duncodi.ppslink.stanchart.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class EncryptDecrypt {

    private static final String AES_ECB_PKCS5PADDING = "AES/ECB/PKCS5Padding";
    private static final String SECRET_KEY = "PiggyPlus32ByteS3cr4tKey@2025X!!"; // 32 bytes for AES-256

    public static String encrypt(String plaintext) {
        try {

            if(!BasicValidationUtil.validateStringForNullAndEmpty(plaintext)){
                return null;
            }

            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed for: " + plaintext, e);
        }
    }

    public static String decrypt(String encryptedData) {
        try {

            if(!BasicValidationUtil.validateStringForNullAndEmpty(encryptedData)){
                return null;
            }

            // Decode Base64 and verify length
            byte[] decoded = Base64.getDecoder().decode(encryptedData.trim());
            if (decoded.length % 16 != 0) {
                throw new IllegalArgumentException("Invalid ciphertext length: " + decoded.length + " bytes. Must be multiple of 16");
            }

            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] plaintext = cipher.doFinal(decoded);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid input: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed for: " + encryptedData, e);
        }
    }

    private static SecretKey getSecretKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) { // Validate key length
            throw new IllegalStateException("Invalid AES key length: " + keyBytes.length + " bytes. Must be 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
