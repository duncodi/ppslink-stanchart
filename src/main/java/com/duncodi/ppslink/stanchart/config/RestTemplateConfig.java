package com.duncodi.ppslink.stanchart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${scb.ssl.keystore.path}")
    private String keystorePath;

    @Value("${scb.ssl.keystore.password}")
    private String keystorePassword;

    @Value("${scb.ssl.keystore.type:JKS}")
    private String keystoreType;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return createRestTemplateWithTimeout();
    }

    @Bean
    public RestTemplate directRestTemplate() {
        return createRestTemplateWithTimeout();
    }

    @Bean(name = "scbRestTemplate")
    public RestTemplate scbRestTemplate() {
        try {
            log.info("🔐 Configuring SCB REST Template with SSL client authentication...");
            log.info("Keystore path: {}", keystorePath);
            log.info("Keystore type: {}", keystoreType);

            // Configure SSL context with client certificate
            SSLContext sslContext = createSSLContext();

            // Create custom request factory with SSL context
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());

                        // Set hostname verifier to be more permissive for testing
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };

            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);

            log.info("✅ SCB REST Template configured successfully with SSL client authentication");
            return new RestTemplate(factory);

        } catch (Exception e) {
            log.error("❌ Failed to configure SSL client authentication: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure SSL for SCB API: " + e.getMessage(), e);
        }
    }

    private SSLContext createSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        if (keystorePath != null && !keystorePath.trim().isEmpty()) {
            java.io.File keystoreFile = new java.io.File(keystorePath);
            if (keystoreFile.exists()) {
                log.info("🔑 Loading client keystore from: {}", keystorePath);
                log.info("🔑 Keystore type: {}", keystoreType);

                // Use the configured keystore type (JKS)
                KeyStore keyStore = KeyStore.getInstance(keystoreType);
                char[] password = (keystorePassword != null && !keystorePassword.trim().isEmpty())
                        ? keystorePassword.toCharArray() : null;

                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis, password);
                }

                // Log available aliases for debugging
                log.info("🔍 Available aliases in keystore:");
                var aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    log.info("   - {}", alias);
                    if (keyStore.isKeyEntry(alias)) {
                        log.info("     ✅ Contains private key");
                    }
                }

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, password);

                // Initialize SSL context with key managers
                sslContext.init(keyManagerFactory.getKeyManagers(), createTrustAllManager(), new SecureRandom());
                log.info("✅ SSL Context initialized with client certificate");

            } else {
                log.error("❌ Keystore file not found: {}", keystorePath);
                throw new RuntimeException("Keystore file not found: " + keystorePath);
            }
        } else {
            log.error("❌ No keystore path configured");
            throw new RuntimeException("No keystore path configured for SCB SSL");
        }

        return sslContext;
    }

    /**
     * Create a trust manager that trusts all certificates (for testing only)
     * In production, use proper certificate validation
     */
    private TrustManager[] createTrustAllManager() {
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // Trust all client certificates
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Trust all server certificates
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
    }

    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }
}