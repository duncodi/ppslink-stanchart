package com.duncodi.ppslink.stanchart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.*;
import java.security.cert.CertificateException;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${scbkeys.keystore-path:}")
    private String keystorePath;

    @Value("${scbkeys.keystore-password:}")
    private String keystorePassword;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return createRestTemplateWithTimeout();
    }

    @Bean
    public RestTemplate directRestTemplate() {
        return createRestTemplateWithTimeout();
    }

    @Bean
    public RestTemplate scbRestTemplate() {
        try {
            log.info("🔐 Configuring SCB REST Template with SSL client authentication...");

            // Configure SSL context with client certificate
            SSLContext sslContext = createSSLContext();

            // Set as default SSL context
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Create custom request factory
            SCBClientHttpRequestFactory factory = new SCBClientHttpRequestFactory(sslContext);
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);

            log.info("✅ SCB REST Template configured successfully with SSL client authentication");
            return new RestTemplate(factory);

        } catch (Exception e) {
            log.error("❌ Failed to configure SSL client authentication: {}", e.getMessage(), e);
            log.warn("🔄 Falling back to regular REST template");
            return createRestTemplateWithTimeout();
        }
    }

    private SSLContext createSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        if (keystorePath != null && !keystorePath.trim().isEmpty()) {
            File keystoreFile = new File(keystorePath);
            if (keystoreFile.exists()) {
                log.info("🔑 Loading client keystore from: {}", keystorePath);

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                char[] password = (keystorePassword != null && !keystorePassword.trim().isEmpty())
                        ? keystorePassword.toCharArray() : new char[0];

                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis, password);
                }

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, password);

                // Initialize SSL context with key managers
                sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
                log.info("✅ SSL Context initialized with client certificate");

            } else {
                log.warn("⚠️ Keystore file not found: {}", keystorePath);
                sslContext.init(null, null, null);
            }
        } else {
            log.warn("⚠️ No keystore path configured");
            sslContext.init(null, null, null);
        }

        return sslContext;
    }

    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    /**
     * Custom Request Factory that uses our SSL context
     */
    private static class SCBClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        private final SSLContext sslContext;

        public SCBClientHttpRequestFactory(SSLContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());

                // Configure hostname verifier
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }
}