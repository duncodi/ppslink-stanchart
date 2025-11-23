package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.ProcessIDsDto;
import com.duncodi.ppslink.stanchart.dto.StraightToBankConfigDto;
import com.duncodi.ppslink.stanchart.dto.UserDto;
import com.duncodi.ppslink.stanchart.enums.CrudOperationType;
import com.duncodi.ppslink.stanchart.enums.YesNo;
import com.duncodi.ppslink.stanchart.exceptions.CustomErrorCode;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.model.StraightToBankConfig;
import com.duncodi.ppslink.stanchart.repository.StraightToBankConfigRepository;
import com.duncodi.ppslink.stanchart.service.otherServices.AdminServiceHelper;
import com.duncodi.ppslink.stanchart.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class StraightToBankConfigService {

    private final RestTemplate directRestTemplate;

    @Qualifier("scbRestTemplate")
    private final RestTemplate scbRestTemplate;

    private final StraightToBankConfigRepository repository;

    private final SftpFileUploadService sftpFileUploadService;

    private final SshAndPgpEncryptionService sshAndPgpEncryptionService;

    private final AuditTrailService auditTrailService;

    private final AdminServiceHelper adminServiceHelper;

    private final StandardCharteredTokenGenerator standardCharteredTokenGenerator;

    public StraightToBankConfigDto findOne(boolean throwSshError){

        List<StraightToBankConfig> configs = repository.findAll();

        StraightToBankConfig config = new StraightToBankConfig();

        log.info("configs found "+configs.size());

        if(!configs.isEmpty()){
            config = configs.getFirst();
        }else{
            log.info("Create a default one...."+config.getId());
            config = repository.save(config);
        }

        return this.convertEntityToDto(config);

    }

    public StraightToBankConfigDto findById(Long id){

        BasicValidationUtil.validateIdentifierAndThrowException(id);

        StraightToBankConfig config = repository.findById(id).orElse(null);

        if(config==null){
            throw new CustomException("Configuration not found using identifier "+id);
        }

        return this.convertEntityToDto(config);

    }

    public StraightToBankConfigDto process(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        log.info("saving configs....");

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        StraightToBankConfig config = this.convertDtoToEntity(request);

        String previousState = "{}";

        if(BasicValidationUtil.validateIdentifierIsOkay(request.getId())){

            StraightToBankConfig existing = repository.findById(request.getId()).orElse(null);

            previousState = existing!=null? JsonUtil.convertToJsonString(existing):"{}";

        }

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

        config = repository.save(config);

        UserDto user = adminServiceHelper.getUserFromToken(accessToken);

        String currentState = JsonUtil.convertToJsonString(config);

        auditTrailService.buildAndSendSingleTrail(CrudOperationType.UPDATE, config.getId(),
                "Standard Chartered Config Saved", previousState, currentState, user, ipAddress, accessToken);

        return this.convertEntityToDto(config);

    }

    public StraightToBankConfigDto update(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        if(!BasicValidationUtil.validateAgainstNullAndZero(request.getId())){
            throw new CustomException("Request has no valid identifier");
        }

        StraightToBankConfig config = repository.findById(request.getId()).orElse(null);

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration not found using identifier "+request.getId());
        }

        return this.process(request, servletRequest);

    }

    public StraightToBankConfigDto convertEntityToDto(StraightToBankConfig config){

        return StraightToBankConfigDto.builder()
                .id(config.getId())
                .schemeCode(config.getSchemeCode())
                .activationKey(config.getActivationKey()!=null?EncryptDecrypt.decrypt(config.getActivationKey()):null)
                .activationUrl(config.getActivationUrl())
                .apiUrl(config.getApiUrl()==null?"https://openbanking.sc.com":config.getApiUrl())
                .paymentInitiationEndPoint(config.getPaymentInitiationEndPoint()==null?"/api/cui/l3-cms/uat2/openapi/payments/v2/initiate"
                        :config.getPaymentInitiationEndPoint())
                .paymentStatusEndPoint(config.getPaymentStatusEndPoint()==null?"/api/cui/l3-cms/uat2/openapi/payments/v2/status"
                        :config.getPaymentStatusEndPoint())
                .urlActivated(config.getUrlActivated()==null?YesNo.NO:config.getUrlActivated())
                .dateUrlActivated(config.getDateUrlActivated()==null?"Not Activated":DateUtil.convertDateToGridLong(config.getDateUrlActivated()))

                .jwtIssuer(config.getJwtIssuer())
                .jwtAudience(config.getJwtAudience())
                .webhookUrl(config.getWebhookUrl())
                .enableWebhook(config.getEnableWebhook())
                .jwtExpiryMinutes(config.getJwtExpiryMinutes())
                .jwtTimeoutSeconds(config.getJwtTimeoutSeconds())

                .activateIntegration(config.getActivateIntegration())
                .promoteToProduction(config.getPromoteToProduction())
                .replaceSpecialChars(config.getReplaceSpecialChars())
                .roundPaymentsTo(config.getRoundPaymentsTo())
                .truncateTrailingDecimals(config.getTruncateTrailingDecimals())

                .build();

    }

    public StraightToBankConfig convertDtoToEntity(StraightToBankConfigDto config){

        return StraightToBankConfig.builder()
                .id(config.getId())
                .schemeCode(config.getSchemeCode())
                .activationKey(config.getActivationKey()!=null?EncryptDecrypt.encrypt(config.getActivationKey()):null)
                .activationUrl(config.getActivationUrl())
                .apiUrl(config.getApiUrl())
                .paymentInitiationEndPoint(config.getPaymentInitiationEndPoint())
                .paymentStatusEndPoint(config.getPaymentStatusEndPoint())

                .jwtIssuer(config.getJwtIssuer())
                .jwtAudience(config.getJwtAudience())
                .webhookUrl(config.getWebhookUrl())
                .enableWebhook(config.getEnableWebhook())
                .jwtExpiryMinutes(config.getJwtExpiryMinutes())
                .jwtTimeoutSeconds(config.getJwtTimeoutSeconds())

                .activateIntegration(config.getActivateIntegration())
                .promoteToProduction(config.getPromoteToProduction())
                .replaceSpecialChars(config.getReplaceSpecialChars())
                .roundPaymentsTo(config.getRoundPaymentsTo())
                .truncateTrailingDecimals(config.getTruncateTrailingDecimals())

                .build();

    }

    public List<StraightToBankConfigDto> getList(){

        List<StraightToBankConfig> list = repository.findAll();

        return list.stream()
                .map(this::convertEntityToDto)
                .toList();

    }

    public String deleteByIds(ProcessIDsDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        List<Long> ids = request.getIds();

        BasicValidationUtil.validateIdentifiersAndThrowCustomEx(ids);

        List<StraightToBankConfig> list = repository.findAllById(ids);

        if(list.isEmpty()){
            throw new CustomException(CustomErrorCode.LIST_404, "No Configs Found");
        }

        String ipAddress = request.getIpAddress();

        int count = 0;

        for(StraightToBankConfig config : list){

            repository.deleteById(config.getId());

            count++;

        }

        return count+" Configuration(s) of Deleted Successfully";

    }

    public StraightToBankConfigDto processUrlActivation(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        log.info("Processing URL Activation ....");

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        if(request.getId()==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Identifier Not Found!");
        }

        List<StraightToBankConfig> configs = repository.findAll();

        log.info("configs>>>>>>>>>>"+configs.isEmpty());

        StraightToBankConfig config = !configs.isEmpty()?configs.getFirst():null;

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found!");
        }

        String activationUrl = config.getActivationUrl();
        String activationKey = config.getActivationKey();

        log.info("activationUrl>>>>>>>>>>"+activationUrl);

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);
        String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

        if(activationUrl==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Activation URL Not Found!");
        }

        if(activationKey==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Activation Key Not Found!");
        }

        String previousState = JsonUtil.convertToJsonString(config);

        try{

            String scbJwtToken = standardCharteredTokenGenerator.generateScbJwtToken(config);

            log.info("=== ACTIVATING SCB SERVICE ===");
            log.info("Activation URL: {}", activationUrl);
            log.info("SCB JWT Token generated successfully, length: {}", scbJwtToken.length());

            // Create proper headers and request body according to SCB documentation
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Routing-Identifier", "ZZ"); // Required header for SCB

            // Create request body with jwtToken as a field (not Bearer token)
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("jwtToken", scbJwtToken);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            log.info("Making POST request to SCB activation endpoint with SSL client authentication...");

            // Use the SSL-enabled SCB REST template
            ResponseEntity<String> response = scbRestTemplate.postForEntity(
                    activationUrl,
                    requestEntity,
                    String.class
            );

            // Log response details
            log.info("=== SCB ACTIVATION RESPONSE ===");
            log.info("HTTP Status: {}", response.getStatusCode());
            log.info("Response Headers: {}", response.getHeaders());

            String responseBody = response.getBody();
            log.info("Response Body: {}", responseBody);

            // Check if activation was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                config.setUrlActivated(YesNo.YES);
                config.setDateUrlActivated(new Date());
                config = repository.save(config);

                log.info("✅ SCB Activation Successful!");
            } else {
                log.error("❌ SCB Activation Failed with status: {}", response.getStatusCode());
                throw new CustomException("SCB Activation failed with status: " + response.getStatusCode());
            }

            UserDto user = adminServiceHelper.getUserFromToken(JWTUtil.getTokenFromServletRequest(servletRequest));

            String currentState = JsonUtil.convertToJsonString(config);

            auditTrailService.buildAndSendSingleTrail(CrudOperationType.UPDATE, config.getId(),
                    "API URL Activated for Standard Chartered Bank", previousState, currentState,
                    user, ipAddress, accessToken);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e){
            log.error("❌ SCB Activation Error: {}", e.getMessage(), e);
            // Provide more specific error message
            String errorMessage = "SCB Activation failed: " + e.getMessage();
            if (e.getMessage().contains("Connection reset") || e.getMessage().contains("SSL")) {
                errorMessage += ". This may be due to missing client certificate authentication. Please ensure SSL certificates are properly configured.";
            }
            throw new CustomException(errorMessage);
        }

        return this.convertEntityToDto(config);
    }

    @Transactional
    public String performTest(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        log.info("🧪 Starting comprehensive SCB connectivity diagnostics...");

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        if(request.getId()==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Identifier Not Found!");
        }

        List<StraightToBankConfig> configs = repository.findAll();
        log.info("configs>>>>>>>>>>"+configs.isEmpty());

        StraightToBankConfig config = !configs.isEmpty()?configs.getFirst():null;

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found!");
        }

        try{
            StringBuilder testResults = new StringBuilder();
            testResults.append("=== SCB COMPREHENSIVE DIAGNOSTICS ===\n\n");

            // 1. Test Basic Network Connectivity
            testResults.append("1. 🌐 BASIC NETWORK CONNECTIVITY:\n");
            testResults.append(diagnoseNetworkConnectivity()).append("\n");

            // 2. Keystore Diagnostics
            testResults.append("2. 🔐 KEYSTORE DIAGNOSTICS:\n");
            testResults.append(diagnoseKeystore()).append("\n");

            // 3. Test Connection WITHOUT Client Certificate
            testResults.append("3. 🔄 CONNECTION WITHOUT CLIENT CERTIFICATE:\n");
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Routing-Identifier", "ZZ");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = directRestTemplate.exchange(
                        "https://apitest.standardchartered.com",
                        org.springframework.http.HttpMethod.HEAD,
                        entity,
                        String.class
                );
                testResults.append("   ✅ SUCCESS - Status: ").append(response.getStatusCode()).append("\n");
            } catch (Exception e) {
                testResults.append("   ❌ FAILED: ").append(e.getMessage()).append("\n");
            }
            testResults.append("\n");

            // 4. Test Connection WITH Client Certificate
            testResults.append("4. 🔑 CONNECTION WITH CLIENT CERTIFICATE:\n");
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Routing-Identifier", "ZZ");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = scbRestTemplate.exchange(
                        config.getActivationUrl(),
                        org.springframework.http.HttpMethod.HEAD,
                        entity,
                        String.class
                );
                testResults.append("   ✅ SUCCESS - Status: ").append(response.getStatusCode()).append("\n");
            } catch (Exception e) {
                testResults.append("   ❌ FAILED: ").append(e.getMessage()).append("\n");
                if (e.getMessage().contains("Connection reset")) {
                    testResults.append("   💡 This indicates SSL handshake failure - likely client certificate issue\n");
                }
            }
            testResults.append("\n");

            // 5. Test SSL Handshake with Raw Connection
            testResults.append("5. 🤝 SSL HANDSHAKE TEST (Raw Connection):\n");
            testResults.append(testSSLHandshake()).append("\n");

            // 6. Activation Key Analysis
            testResults.append("6. 📋 ACTIVATION KEY ANALYSIS:\n");
            if (config.getActivationKey() != null) {
                String decryptedKey = EncryptDecrypt.decrypt(config.getActivationKey());
                if (decryptedKey != null) {
                    testResults.append("   ✅ Activation key exists and can be decrypted\n");
                    testResults.append("   📏 Length: ").append(decryptedKey.length()).append(" characters\n");

                    // Analyze format
                    if (decryptedKey.trim().startsWith("{")) {
                        testResults.append("   📄 Format: JSON\n");
                        // Try to parse JSON
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            var jsonNode = mapper.readTree(decryptedKey);
                            boolean hasContent = jsonNode.has("content");
                            boolean hasKey = jsonNode.has("key");
                            testResults.append("   📊 JSON Structure:\n");
                            testResults.append("      - Has 'content' field: ").append(hasContent ? "✅ YES" : "❌ NO").append("\n");
                            testResults.append("      - Has 'key' field: ").append(hasKey ? "✅ YES" : "❌ NO").append("\n");

                            if (hasContent) {
                                String content = jsonNode.get("content").asText();
                                testResults.append("      - Content length: ").append(content.length()).append("\n");
                                testResults.append("      - Content preview: ").append(content.substring(0, Math.min(50, content.length()))).append("...\n");
                            }
                            if (hasKey) {
                                String key = jsonNode.get("key").asText();
                                testResults.append("      - Key length: ").append(key.length()).append("\n");
                                testResults.append("      - Key preview: ").append(key.substring(0, Math.min(30, key.length()))).append("...\n");
                            }
                        } catch (Exception e) {
                            testResults.append("   ❌ Failed to parse JSON: ").append(e.getMessage()).append("\n");
                        }
                    } else if (decryptedKey.contains("BEGIN CERTIFICATE") || decryptedKey.contains("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t")) {
                        testResults.append("   ⚠️  Format: CERTIFICATE (This should be JSON with 'content' and 'key' fields!)\n");
                        testResults.append("   💡 Contact SCB for correct activation key format\n");
                    } else {
                        testResults.append("   ❓ Format: Unknown\n");
                        testResults.append("   📝 Preview: ").append(decryptedKey.substring(0, Math.min(100, decryptedKey.length()))).append("...\n");
                    }
                } else {
                    testResults.append("   ❌ Activation key decryption failed\n");
                }
            } else {
                testResults.append("   ❌ No activation key configured\n");
            }
            testResults.append("\n");

            // 7. JWT Token Generation Test
            testResults.append("7. 🎫 JWT TOKEN GENERATION TEST:\n");
            try {
                String jwtToken = standardCharteredTokenGenerator.generateScbJwtToken(config);
                testResults.append("   ✅ SUCCESS - JWT token generated\n");
                testResults.append("   📏 Token length: ").append(jwtToken.length()).append(" characters\n");
                testResults.append("   🔍 Token preview: ").append(jwtToken.substring(0, Math.min(50, jwtToken.length()))).append("...\n");
            } catch (Exception e) {
                testResults.append("   ❌ FAILED: ").append(e.getMessage()).append("\n");
            }

            // 8. Configuration Summary
            testResults.append("\n8. ⚙️  CONFIGURATION SUMMARY:\n");
            testResults.append("   - Activation URL: ").append(config.getActivationUrl()).append("\n");
            testResults.append("   - JWT Issuer: ").append(config.getJwtIssuer()).append("\n");
            testResults.append("   - JWT Audience: ").append(config.getJwtAudience()).append("\n");
            testResults.append("   - URL Activated: ").append(config.getUrlActivated()).append("\n");
            testResults.append("   - Webhook Enabled: ").append(config.getEnableWebhook()).append("\n");

            String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);
            String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);
            UserDto user = adminServiceHelper.getUserFromToken(accessToken);
            String currentState = JsonUtil.convertToJsonString(config);

            auditTrailService.buildAndSendSingleTrail(CrudOperationType.UPDATE, config.getId(),
                    "SCB Comprehensive Diagnostics Completed", currentState, currentState, user, ipAddress, accessToken);

            log.info("Comprehensive Diagnostics Results:\n{}", testResults.toString());
            return testResults.toString();

        } catch (Exception e){
            log.error("Comprehensive diagnostics failed: {}", e.getMessage(), e);
            throw new CustomException("Comprehensive diagnostics failed: " + e.getMessage());
        }
    }

    /**
     * Diagnose network connectivity issues
     */
    private String diagnoseNetworkConnectivity() {
        StringBuilder result = new StringBuilder();
        try {
            // Test DNS resolution
            Process dnsProcess = Runtime.getRuntime().exec("nslookup apitest.standardchartered.com");
            int dnsExitCode = dnsProcess.waitFor();
            if (dnsExitCode == 0) {
                result.append("   ✅ DNS Resolution: SUCCESS\n");
            } else {
                result.append("   ❌ DNS Resolution: FAILED\n");
            }

            // Test ping
            Process pingProcess = Runtime.getRuntime().exec("ping -c 3 -W 5 apitest.standardchartered.com");
            int pingExitCode = pingProcess.waitFor();
            if (pingExitCode == 0) {
                result.append("   ✅ Ping: SUCCESS\n");
            } else {
                result.append("   ❌ Ping: FAILED\n");
            }

            // Test port connectivity
            Process telnetProcess = Runtime.getRuntime().exec("timeout 5 bash -c 'echo \"\" | telnet apitest.standardchartered.com 443'");
            int telnetExitCode = telnetProcess.waitFor();
            if (telnetExitCode == 0) {
                result.append("   ✅ Port 443: REACHABLE\n");
            } else {
                result.append("   ❌ Port 443: BLOCKED\n");
            }

        } catch (Exception e) {
            result.append("   ❌ Network diagnosis failed: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }

    /**
     * Test SSL handshake with client certificate
     */
    private String testSSLHandshake() {
        try {
            String keystorePath = "/etc/ppslinkkeys/stanchart_bank_certs/www.pps.go.ug.p12";
            String password = System.getenv("STANCHART_KEYSTORE_PASSWORD");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] passwordChars = (password != null && !password.trim().isEmpty()) ? password.toCharArray() : new char[0];

            try (InputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, passwordChars);
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, passwordChars);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

            URL url = new URL("https://apitest.standardchartered.com");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            return "   ✅ SSL Handshake Successful - Response Code: " + responseCode;

        } catch (Exception e) {
            return "   ❌ SSL Handshake Failed: " + e.getMessage();
        }
    }

    private String diagnoseKeystore() {
        StringBuilder result = new StringBuilder();
        try {
            String keystorePath = "/etc/ppslinkkeys/stanchart_bank_certs/www.pps.go.ug.p12";
            String password = System.getenv("STANCHART_KEYSTORE_PASSWORD");

            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists()) {
                return "   ❌ Keystore file not found: " + keystorePath + "\n";
            }

            result.append("   ✅ Keystore file exists: ").append(keystorePath).append("\n");
            result.append("   📁 File size: ").append(keystoreFile.length()).append(" bytes\n");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] passwordChars = (password != null && !password.trim().isEmpty()) ? password.toCharArray() : new char[0];

            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, passwordChars);
                result.append("   🔐 Keystore loaded successfully\n");
                result.append("   📊 Keystore type: ").append(keyStore.getType()).append("\n");
                result.append("   🔢 Keystore size: ").append(keyStore.size()).append(" entries\n");

                Enumeration<String> aliases = keyStore.aliases();
                boolean hasEntries = false;

                while (aliases.hasMoreElements()) {
                    hasEntries = true;
                    String alias = aliases.nextElement();
                    result.append("   ┌─ Alias: ").append(alias).append("\n");

                    // Check certificate
                    Certificate cert = keyStore.getCertificate(alias);
                    if (cert != null) {
                        result.append("   │  📜 Certificate type: ").append(cert.getType()).append("\n");
                        if (cert instanceof X509Certificate) {
                            X509Certificate x509 = (X509Certificate) cert;
                            result.append("   │  👤 Subject: ").append(x509.getSubjectX500Principal().getName()).append("\n");
                            result.append("   │  🏢 Issuer: ").append(x509.getIssuerX500Principal().getName()).append("\n");
                            result.append("   │  📅 Valid from: ").append(x509.getNotBefore()).append("\n");
                            result.append("   │  📅 Valid until: ").append(x509.getNotAfter()).append("\n");

                            // Check if certificate is valid
                            try {
                                x509.checkValidity();
                                result.append("   │  ✅ Certificate is valid\n");
                            } catch (Exception e) {
                                result.append("   │  ❌ Certificate validation failed: ").append(e.getMessage()).append("\n");
                            }
                        }
                    } else {
                        result.append("   │  ❌ No certificate found for this alias\n");
                    }

                    // Check private key
                    try {
                        Key key = keyStore.getKey(alias, passwordChars);
                        if (key != null) {
                            result.append("   │  🔑 Private key algorithm: ").append(key.getAlgorithm()).append("\n");
                            result.append("   │  📐 Private key format: ").append(key.getFormat()).append("\n");
                        } else {
                            result.append("   │  ❌ No private key found for this alias\n");
                        }
                    } catch (Exception e) {
                        result.append("   │  ❌ Failed to access private key: ").append(e.getMessage()).append("\n");
                    }
                    result.append("   └─────────────────────────────────────────\n");
                }

                if (!hasEntries) {
                    result.append("   ❌ No entries found in keystore\n");
                }
            }

        } catch (Exception e) {
            result.append("   ❌ Keystore diagnosis failed: ").append(e.getMessage()).append("\n");
            if (e.getMessage().contains("password")) {
                result.append("   💡 Check STANCHART_KEYSTORE_PASSWORD environment variable\n");
            }
        }
        return result.toString();
    }
}