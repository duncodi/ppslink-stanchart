package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.*;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailRequestDto;
import com.duncodi.ppslink.stanchart.enums.*;
import com.duncodi.ppslink.stanchart.exceptions.CustomErrorCode;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.model.StraightToBankBatch;
import com.duncodi.ppslink.stanchart.model.StraightToBankBatchLine;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchLineRepository;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchRepository;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchSpecifications;
import com.duncodi.ppslink.stanchart.repository.StraightToBankLineRepository;
import com.duncodi.ppslink.stanchart.service.otherServices.AdminServiceHelper;
import com.duncodi.ppslink.stanchart.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StraightToBankService {

    private final StraightToBankBatchLineRepository bankBatchLineRepository;
    private final StraightToBankBatchRepository batchRepository;
    private final StraightToBankXMLGeneratorService xmlGeneratorService;
    private final StraightToBankConfigService configService;
    private final SshAndPgpEncryptionService sshAndPgpEncryptionService;
    private final SftpFileUploadService sftpFileUploadService;
    private final AdminServiceHelper adminServiceHelper;
    private final AuditTrailService auditTrailService;
    private final StraightToBankLineRepository lineRepository;
    private final ObjectMapper objectMapper;
    private final StandardCharteredTokenGenerator standardCharteredTokenGenerator;

    @Qualifier("scbRestTemplate")
    private final RestTemplate scbRestTemplate;

    public StraightToBankPayloadDto getNextMessageId(StraightToBankPayloadDto payload){

        Long messageIdSeq = batchRepository.findMaxMessageIdSeqNative()+1;

        String msgIdPadded = NumbersUtil.leftPadNumber(messageIdSeq, 7);
        String schemeCode = payload.getSchemeCode()==null?"PPS":payload.getSchemeCode();
        Date valueDate = DateUtil.convertStringToDate(payload.getValueDate());
        String preparerInitials = payload.getPreparerInitials();

        if(preparerInitials==null){
            preparerInitials = (payload.getPreparedByName()==null?"PL":payload.getPreparedByName()).substring(0, 4);
        }

        int year = DateUtil.getYearFromDate(valueDate);
        CustomMonth month = DateUtil.getMonthFromDate(valueDate);

        String messageId = (schemeCode+month.name()+year+msgIdPadded+preparerInitials).toUpperCase();

        messageId = StringUtil.tameStringCustom(StringUtil.replaceSpecialCharactersLeave(messageId), 28);

        payload.setMessageIdSeq(messageIdSeq);
        payload.setMessageId(messageId);

        log.info("messageId>>>>"+messageId);

        return payload;

    }

    public StraightToBankBatchResponseDto findById(Long id){

        BasicValidationUtil.validateIdentifierAndThrowException(id);

        StraightToBankBatch batch = batchRepository.findById(id).orElse(null);

        if(batch==null){
            throw new CustomException("Batch not found using identifier "+id);
        }

        return this.convertEntityToResponseDto(batch);

    }

    @Transactional
    public StraightToBankResultsDto process(StraightToBankPayloadDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        UserDto actor = adminServiceHelper.getUserFromToken(JWTUtil.getTokenFromServletRequest(servletRequest));

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        request = this.getNextMessageId(request);

        StraightToBankBatch batch = this.convertPaymentDtoToEntity(request);

        String schemeCode = batch.getSchemeCode();

        if(schemeCode==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Scheme Code Not Provided on Payload");
        }

        StraightToBankConfigDto config = configService.findOne(true);

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found for Scheme "+schemeCode);
        }

       /* if (!YesNo.YES.equals(config.getUrlActivated())) {
            throw new CustomException("SCB API URL is not activated. Please activate the URL first.");
        }*/

        log.info("Processing Payment File ...."+batch.getMessageId());

        List<StraightToBankPayloadLineDto> linesAll = request.getLines()==null?new ArrayList<>():request.getLines();

        if(linesAll.isEmpty()){
            throw new CustomException(CustomErrorCode.REQ_404, "Payment Lines Not Provided");
        }

        List<StraightToBankBatchLine> linesConverted = linesAll.stream()
                .map(this::convertLineDtoToEntity)
                .toList();

        List<StraightToBankBatchLine> linesWithoutGroup = linesConverted.stream()
                .filter(l->l.getInstructionGroup()==null)
                .toList();

        List<StraightToBankBatchLine> linesWithGroup = linesConverted.stream()
                .filter(l->l.getInstructionGroup()!=null)
                .toList();

        String defaultGroupCode = StringUtil.tameStringCustom(batch.getMessageId()+StringUtil.generateRandomString(4), 35).toUpperCase();

        for(StraightToBankBatchLine line : linesWithoutGroup){

            line.setInstructionGroup(defaultGroupCode);

        }

        for(StraightToBankBatchLine line : linesWithGroup){

            String grp = line.getInstructionGroup();
            String updatedGroupCode = StringUtil.tameStringCustom(batch.getMessageId()+grp, 35).toUpperCase();

            line.setInstructionGroup(updatedGroupCode);

        }

        List<StraightToBankBatchLine> lines = new ArrayList<>(linesWithGroup);

        if(!linesWithoutGroup.isEmpty()){
            lines.addAll(linesWithoutGroup);
        }

        batch.addLines(lines);

        String apiUrl = config.getApiUrl();

        if(apiUrl==null){
            throw new CustomException("API Url Not Found!");
        }

        String endPoint = config.getPaymentInitiationEndPoint();

        if(endPoint==null){
            throw new CustomException("Payment Initiation Endpoint Not Found!");
        }

        String paymentUrl = apiUrl+endPoint;

        log.info("SCB PAYMENT URL: {}", paymentUrl);

        String jsonString = "{}";
        String deliveryRes = "FAILED";
        String statusCode = "500";
        String scbResponse = "";
        Map<String, String> responseHeaders = new HashMap<>();
        String correlationId = "";

        try {

            ObjectNode stanchartJson = this.generateJsonFile(batch, config);
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stanchartJson);

            log.info("Generated JSON Payload for messageId: {}", batch.getMessageId());

        } catch (Exception e){
            log.error("Error generating JSON payload", e);
            throw new CustomException(CustomErrorCode.INVALID_JSON, "Failed to generate JSON payload: " + e.getMessage());
        }

        try {

            String scbJwtToken = "XXXXXXXXXXXXXX";//standardCharteredTokenGenerator.generateScbJwtToken(config);

            log.info("SCB JWT Token generated successfully, length: {}", scbJwtToken.length());

            HttpHeaders headers = this.createSCBRequestHeaders(config, batch.getMessageId(), scbJwtToken);

            HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);

            log.info("=== SENDING PAYMENT TO SCB API ===");
            log.info("URL: {}", paymentUrl);
            log.info("Message ID: {}", batch.getMessageId());
            log.info("Request Headers: {}", headers);

            ResponseEntity<String> response = scbRestTemplate.exchange(
                    paymentUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            statusCode = String.valueOf(response.getStatusCode().value());
            scbResponse = response.getBody();

            log.info("=== SCB PAYMENT RESPONSE ===");
            log.info("HTTP Status: {}", response.getStatusCode());
            log.info("Response Body: {}", scbResponse);

            if (response.getStatusCode().is2xxSuccessful()) {
                deliveryRes = "SUCCESS";
                log.info("✅ Payment successfully delivered to SCB. Status: {}", statusCode);
            } else {
                deliveryRes = "FAILED";
                log.warn("❌ Failed to deliver payment to SCB. Status: {}, Correlation ID: {}, Response: {}",
                        statusCode, correlationId, scbResponse);
            }

        } catch (Exception e) {

            log.error("❌ Error delivering payment to SCB API", e);

            String errorMessage = "Failed to Deliver Payment File to Standard Chartered: " + e.getMessage();

            if (e.getMessage().contains("Connection reset") || e.getMessage().contains("SSL")) {
                errorMessage += ". This may be due to SSL certificate issues. Please ensure SSL certificates are properly configured.";
            }

            throw new CustomException(errorMessage);

        }

        String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

        // Save batch with delivery status
        batch.setDeliveryStatus(deliveryRes);
        batch.setStatusCode(statusCode);
        batch.setApiResponse(scbResponse);
        batch.setJsonRequest(jsonString);

        batch = batchRepository.save(batch);

        // Audit trail for batch creation
        auditTrailService.buildAndSendSingleTrail(CrudOperationType.CREATE, actor.getId(),
                "Straight to Bank Payment Saved and Sent to SCB - Status: " + deliveryRes + ", Correlation ID: " + correlationId,
                JsonUtil.convertToJsonString(batch),
                JsonUtil.convertToJsonString(batch), actor, ipAddress, accessToken);

        // Audit trails for individual lines
        List<AuditTrailRequestDto> auditList = new ArrayList<>();
        for(StraightToBankBatchLine line : batch.getLines()){

            String currentState = JsonUtil.convertToJsonString(line);
            String objectDescription = "Straight to Bank Payment Line " + line.getParticulars() + " Saved - SCB Delivery: " + deliveryRes;

            AuditTrailRequestDto auditTrailRequest = auditTrailService.buildAuditTrailRequest(CrudOperationType.CREATE, line.getId(),
                    objectDescription, currentState, currentState, actor, ipAddress);

            auditList.add(auditTrailRequest);
        }

        auditTrailService.sendAuditTrail(auditList, accessToken);

        // Prepare results
        StraightToBankResultsDto results = new StraightToBankResultsDto();
        results.setStatus("Payment processing completed - SCB Delivery: " + deliveryRes);
        results.setStatusCode(statusCode);
        results.setBatchId(batch.getId());
        results.setMessageId(batch.getMessageId());
        results.setDeliveryResponse(scbResponse);

        if ("FAILED".equals(deliveryRes)) {
            results.setStatus("Failed to send payment file to Standard Chartered Bank");
        } else {
            results.setStatus("Successfully sent payment file to Standard Chartered Bank");
        }

        log.info("Payment processing completed. Batch ID: {}, Status: {}, Correlation ID: {}",
                batch.getId(), deliveryRes, correlationId);

        return results;
    }

    private HttpHeaders createSCBRequestHeaders(StraightToBankConfigDto config, String messageId, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();

        // Required headers based on SCB API documentation
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // SCB specific headers
        headers.set("Routing-Identifier", "ZZ");

        headers.set("X-Request-ID", messageId);

        // Correlation ID for tracking
        headers.set("X-Correlation-ID", UUID.randomUUID().toString());

        // Security headers
        headers.set("Cache-Control", "no-cache");
        headers.set("Pragma", "no-cache");

        // Timestamp
        headers.set("X-Timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));

        log.debug("Created SCB request headers for message: {}", messageId);

        return headers;
    }

    /**
     * Alternative method that sends payment without wrapping in JWT token structure
     * Use this if SCB expects the payment payload directly in the request body
     */
    private StraightToBankResultsDto sendPaymentDirect(StraightToBankBatch batch, StraightToBankConfigDto config, String jsonString) {
        try {
            // Generate JWT token for SCB authentication
            String scbJwtToken = standardCharteredTokenGenerator.generateScbJwtToken(config);
            log.info("SCB JWT Token generated successfully, length: {}", scbJwtToken.length());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Routing-Identifier", "ZZ");
            headers.set("Authorization", "Bearer " + scbJwtToken);
            headers.set("X-Request-ID", batch.getMessageId());
            headers.set("X-Correlation-ID", UUID.randomUUID().toString());

            HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);

            log.info("Sending direct payment to SCB API...");

            ResponseEntity<String> response = scbRestTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String statusCode = String.valueOf(response.getStatusCode().value());
            String scbResponse = response.getBody() != null ? response.getBody() : "No response body";
            String correlationId = response.getHeaders().getFirst("x-correlation-id");

            StraightToBankResultsDto results = new StraightToBankResultsDto();
            results.setStatusCode(statusCode);
            results.setDeliveryResponse(scbResponse);
            results.setMessageId(batch.getMessageId());

            if (response.getStatusCode().is2xxSuccessful()) {
                results.setStatus("SUCCESS");
                log.info("✅ Direct payment successful. Status: {}, Correlation ID: {}", statusCode, correlationId);
            } else {
                results.setStatus("FAILED");
                log.warn("❌ Direct payment failed. Status: {}, Correlation ID: {}", statusCode, correlationId);
            }

            return results;

        } catch (Exception e) {
            log.error("❌ Error in direct payment to SCB API", e);
            throw new CustomException("Direct payment failed: " + e.getMessage());
        }
    }

    public ObjectNode generateJsonFile(StraightToBankBatch batch, StraightToBankConfigDto config) throws Exception {

        List<StraightToBankBatchLine> lines = batch.getLines();

        if (lines == null || lines.isEmpty()) {
            throw new CustomException("No payment lines found for batch: " + batch.getMessageId());
        }

        // For now, we'll use the first line to generate the JSON payload
        // In a real scenario, you might want to handle multiple lines differently
        StraightToBankBatchLine fl = lines.getFirst();

        // Create the main JSON object
        ObjectNode rootNode = objectMapper.createObjectNode();

        // Create header object
        ObjectNode headerNode = objectMapper.createObjectNode();
        headerNode.put("messageId", batch.getMessageId() != null ? batch.getMessageId() : "UGPAYMWPeyRxMOnb");
        headerNode.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));
        headerNode.put("countryCode", batch.getCountryCode() != null ? batch.getCountryCode() : "KE");

        rootNode.set("header", headerNode);

        // Create instruction object
        ObjectNode instructionNode = objectMapper.createObjectNode();

        // Amount object (required)
        ObjectNode amountNode = objectMapper.createObjectNode();
        amountNode.put("amount", fl.getAmountBc() != null ? fl.getAmountBc().doubleValue() : 500.0);
        amountNode.put("currencyCode", fl.getTransactionCurrency() != null ? fl.getTransactionCurrency() : "KES");

        instructionNode.set("amount", amountNode);

        // Debtor object (required)
        ObjectNode debtorNode = objectMapper.createObjectNode();
        debtorNode.put("name", batch.getSchemeName() != null ? batch.getSchemeName() : "SCB");

        instructionNode.set("debtor", debtorNode);

        // Purpose (required)
        instructionNode.put("purpose", "CASH");

        // Creditor object (required)
        ObjectNode creditorNode = objectMapper.createObjectNode();
        creditorNode.put("name", fl.getBeneficiaryName() != null ? fl.getBeneficiaryName() : "Beneficiary");

        ObjectNode creditorAddressNode = objectMapper.createObjectNode();
        creditorAddressNode.put("country", fl.getBeneficiaryCountryCode() != null ? fl.getBeneficiaryCountryCode() : "KE");
        creditorAddressNode.put("city", extractCityFromAddress(fl.getBeneficiaryAddress())); // Extract from address

        creditorNode.set("postalAddress", creditorAddressNode);
        instructionNode.set("creditor", creditorNode);

        // Debtor Agent (required)
        ObjectNode debtorAgentNode = objectMapper.createObjectNode();
        ObjectNode debtorFinancialInstitutionNode = objectMapper.createObjectNode();
        debtorFinancialInstitutionNode.put("BIC", batch.getCashbookSwiftCode() != null ? batch.getCashbookSwiftCode() : "SCBLKENXXXX");
        debtorFinancialInstitutionNode.put("name", batch.getCashbookName() != null ? batch.getCashbookName() : "Standard Chartered Bank");

        ObjectNode debtorAddressNode = objectMapper.createObjectNode();
        debtorAddressNode.put("country", batch.getCountryCode() != null ? batch.getCountryCode() : "KE");
        // City in debtorAgent is optional - include only if available
        String debtorCity = extractDebtorAgentCity(batch);
        if (!debtorCity.isEmpty()) {
            debtorAddressNode.put("city", debtorCity);
        }

        debtorFinancialInstitutionNode.set("postalAddress", debtorAddressNode);
        debtorAgentNode.set("financialInstitution", debtorFinancialInstitutionNode);
        instructionNode.set("debtorAgent", debtorAgentNode);

        // Payment Type (required)
        instructionNode.put("paymentType", determinePaymentType(fl));

        // Reference ID (required)
        instructionNode.put("referenceId", fl.getRefCode() != null ? fl.getRefCode() : generateReferenceId(batch));

        // Charge Bearer (required) - correct spelling
        instructionNode.put("chargeBearer", determineChargeBearer(batch));

        // Creditor Agent (required)
        ObjectNode creditorAgentNode = objectMapper.createObjectNode();
        ObjectNode creditorFinancialInstitutionNode = objectMapper.createObjectNode();
        creditorFinancialInstitutionNode.put("BIC", determineCreditorBic(fl));
        creditorFinancialInstitutionNode.put("name", fl.getBankName() != null ? fl.getBankName() : "Bank");

        ObjectNode creditorAgentAddressNode = objectMapper.createObjectNode();
        creditorAgentAddressNode.put("country", fl.getBankCountryCode() != null ? fl.getBankCountryCode() : "KE");
        creditorAgentAddressNode.put("city", determineCreditorAgentCity(fl));

        creditorFinancialInstitutionNode.set("postalAddress", creditorAgentAddressNode);
        creditorAgentNode.set("financialInstitution", creditorFinancialInstitutionNode);
        instructionNode.set("creditorAgent", creditorAgentNode);

        // Debtor Account (required)
        ObjectNode debtorAccountNode = objectMapper.createObjectNode();
        debtorAccountNode.put("id", batch.getDebitAccountNo() != null ? batch.getDebitAccountNo() : "2345678904");
        debtorAccountNode.put("identifierType", "BBAN");
        // Add currency only for foreign currency payments
        if (isForeignCurrencyPayment(fl)) {
            debtorAccountNode.put("currency", fl.getTransactionCurrency());
        }
        instructionNode.set("debtorAccount", debtorAccountNode);

        // Creditor Account (required)
        ObjectNode creditorAccountNode = objectMapper.createObjectNode();
        creditorAccountNode.put("id", fl.getAccountNo() != null ? fl.getAccountNo() : "256777254583");
        creditorAccountNode.put("identifierType", "Other");
        // Add currency only for foreign currency payments
        if (isForeignCurrencyPayment(fl)) {
            creditorAccountNode.put("currency", fl.getTransactionCurrency());
        }
        instructionNode.set("creditorAccount", creditorAccountNode);

        // Remittance Info (optional)
        if (hasRemittanceInfo(fl)) {
            ObjectNode remittanceInfoNode = objectMapper.createObjectNode();
            ArrayNode multiUnstructuredNode = objectMapper.createArrayNode();
            // Add remittance details from line particulars
            String[] remittanceLines = extractRemittanceInfo(fl.getParticulars());
            for (String remittance : remittanceLines) {
                if (remittance != null && !remittance.trim().isEmpty()) {
                    multiUnstructuredNode.add(remittance.trim());
                }
            }
            if (!multiUnstructuredNode.isEmpty()) {
                remittanceInfoNode.set("multiUnstructured", multiUnstructuredNode);
                instructionNode.set("remittanceInfo", remittanceInfoNode);
            }
        }

        // Timestamp and dates (required)
        instructionNode.put("paymentTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));
        instructionNode.put("paymentTypePreference", "Explicit");
        instructionNode.put("requiredExecutionDate", new SimpleDateFormat("yyyy-MM-dd").format(batch.getValueDate() != null ? batch.getValueDate() : new Date()));

        rootNode.set("instruction", instructionNode);

        log.info("Generated standardized SCB JSON payload for payment: {}", fl.getRefCode());
        return rootNode;
    }

    // Helper methods for the standardized structure
    private String extractCityFromAddress(String beneficiaryAddress) {
        if (beneficiaryAddress == null || beneficiaryAddress.isEmpty()) {
            return "Nairobi"; // Default fallback
        }
        // Simple extraction - you might want to implement more sophisticated logic
        String[] addressParts = beneficiaryAddress.split(",");
        if (addressParts.length > 0) {
            return addressParts[addressParts.length - 1].trim(); // Assume city is last part
        }
        return "Nairobi";
    }

    private String extractDebtorAgentCity(StraightToBankBatch batch) {
        // Extract city from debtor agent information
        // This could come from batch.getCashbookBranchCode() or other fields
        return "NBO"; // Default for now
    }

    private String determinePaymentType(StraightToBankBatchLine line) {
        // Determine payment type based on transaction details
        if (line.getPaymentType() != null) {
            return line.getPaymentType();
        }
        // Default based on currency/country
        if (isForeignCurrencyPayment(line)) {
            return "TT"; // Telegraphic Transfer for foreign currency
        }
        return "PAY"; // Domestic payment
    }

    private String determineChargeBearer(StraightToBankBatch batch) {
        // Determine charge bearer based on batch configuration
        if (batch.getChargeBearer() != null) {
            return batch.getChargeBearer().name(); // Assuming enum
        }
        return "DEBT"; // Default: debtor bears charges
    }

    private String determineCreditorBic(StraightToBankBatchLine line) {
        // Determine creditor BIC based on bank details
        if (line.getSwiftCode() != null && !line.getSwiftCode().equals("-")) {
            return line.getSwiftCode();
        }
        // Fallback logic based on bank name/country
        if (line.getBankName() != null && line.getBankName().toLowerCase().contains("standard chartered")) {
            return "SCBLKENXXXX";
        }
        return "MPESA"; // Default fallback
    }

    private String determineCreditorAgentCity(StraightToBankBatchLine line) {
        // Extract city from bank details or beneficiary address
        if (line.getBeneficiaryAddress() != null) {
            return extractCityFromAddress(line.getBeneficiaryAddress());
        }
        return "Nairobi"; // Default
    }

    private boolean isForeignCurrencyPayment(StraightToBankBatchLine line) {
        // Check if this is a foreign currency payment
        String currency = line.getTransactionCurrency();
        String country = line.getBeneficiaryCountryCode();

        // If currency is different from debtor's base currency, it's foreign
        // You might want to add more sophisticated logic here
        return currency != null &&
                !currency.equals("UGX") &&
                !currency.equals(line.getBaseCurrencyCode());
    }

    private boolean hasRemittanceInfo(StraightToBankBatchLine line) {
        // Check if there's remittance information available
        return line.getParticulars() != null && !line.getParticulars().isEmpty();
    }

    private String[] extractRemittanceInfo(String particulars) {
        if (particulars == null || particulars.isEmpty()) {
            return new String[0];
        }
        // Split particulars into multiple lines for remittance info
        // You might want to implement more sophisticated parsing
        return particulars.split("\\|"); // Assuming | is delimiter, adjust as needed
    }

    private String generateReferenceId(StraightToBankBatch batch) {
        // Generate a unique reference ID if not provided
        return "REF" + batch.getMessageId() + System.currentTimeMillis();
    }

    public StraightToBankBatchResponseDto convertEntityToResponseDto(StraightToBankBatch batch){

        return StraightToBankBatchResponseDto.builder()
                .id(batch.getId())
                .messageId(batch.getMessageId())
                .messageIdSeq(batch.getMessageIdSeq())
                .schemeId(batch.getSchemeId())
                .schemeName(batch.getSchemeName())
                .schemeCode(batch.getSchemeCode())
                .batchDate(DateUtil.convertDateToGridShort(batch.getBatchDate()))
                .preparedById(batch.getPreparedById())
                .preparedByName(batch.getPreparedByName())
                .cashbookId(batch.getCashbookId())
                .cashbookName(batch.getCashbookName())
                .cashbookAccountName(batch.getDebitAccountName())
                .cashbookAccountNo(batch.getDebitAccountNo())
                .cashbookSwiftCode(batch.getCashbookSwiftCode())
                .countLines(batch.getCountTransactions())
                .totalSourceCurrency(batch.getTotal())
                .totalBaseCurrency(batch.getTotalBc())
                .consolidatePaymentLines(batch.getConsolidatedPosting())
                .instructionPriority(batch.getInstructionPriority())
                .valueDate(DateUtil.convertDateToGridShort(batch.getValueDate()))
                .chargeBearer(batch.getChargeBearer())
                .batchTitle(batch.getBatchTitle())
                .deliveryStatus(batch.getDeliveryStatus())
                .statusCode(batch.getStatusCode())
                .apiResponse(batch.getApiResponse())
                .build();

    }

    public StraightToBankLineResponseDto convertLineEntityToResponseDto(StraightToBankBatchLine line){

        return StraightToBankLineResponseDto.builder()
                .id(line.getId())
                .paymentId(line.getPaymentId())
                .batchPaymentScheduleId(line.getBatchPaymentScheduleId())
                .customerRef(line.getCustomerRef())
                .amountSc(line.getAmountSc())
                .amountBc(line.getAmountBc())
                .spotRate(line.getSpotRate())
                .currencyId(line.getCurrencyId())
                .paymentCurrencyCode(line.getPaymentCurrencyCode())
                .baseCurrencyCode(line.getBaseCurrencyCode())
                .purposeOfPayment(line.getPurposeOfPayment())
                .forexType(line.getForexType())
                .forexDealNo(line.getForexDealNo())
                .forexDealerName(line.getForexDealerName())
                .forexDirectInverse(line.getForexDirectInverse())
                .maturityDate(line.getMaturityDate() != null ? DateUtil.convertDateToGridShort(line.getMaturityDate()) : null)
                .intermediaryBankCode(line.getIntermediaryBankCode())
                .localBankCode(line.getLocalBankCode())
                .bankCode(line.getBankCode())
                .branchCode(line.getBranchCode())
                .bankName(line.getBankName())
                .swiftCode(line.getSwiftCode())
                .bankCountryCode(line.getBankCountryCode())
                .accountName(line.getAccountName())
                .accountNo(line.getAccountNo())
                .beneficiaryName(line.getBeneficiaryName())
                .email(line.getEmail())
                .particulars(line.getParticulars())
                .debitAccountNo(line.getDebitAccountNo())
                .debitAccountName(line.getDebitAccountName())
                .paymentType(line.getPaymentType())
                .refCode(line.getRefCode())
                .transactionCurrency(line.getTransactionCurrency())
                .beneficiaryAddress(line.getBeneficiaryAddress())
                .beneficiaryCountryCode(line.getBeneficiaryCountryCode())
                .beneficiaryCountryName(line.getBeneficiaryCountryName())
                .jsonRequest(line.getJsonRequest())
                .recallInitiatedById(line.getRecallInitiatedById())
                .recallInitiatedByName(line.getRecallInitiatedByName())
                .recallInitiatedDate(line.getRecallInitiatedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallInitiatedDate()) : null)
                .recallCertifiedById(line.getRecallCertifiedById())
                .recallCertifiedByName(line.getRecallCertifiedByName())
                .recallCertifiedDate(line.getRecallCertifiedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallCertifiedDate()) : null)
                .recallApprovedById(line.getRecallApprovedById())
                .recallApprovedByName(line.getRecallApprovedByName())
                .recallApprovedDate(line.getRecallApprovedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallApprovedDate()) : null)
                .comments(line.getComments())
                .oldPaymentId(line.getOldPaymentId())
                .instructionGroup(line.getInstructionGroup())
                .build();

    }

    public StraightToBankBatch convertPaymentDtoToEntity(StraightToBankPayloadDto batch){

        YesNo consolidateLines = YesNo.YES.name().equalsIgnoreCase(batch.getConsolidatePaymentLines())?YesNo.YES:YesNo.NO;
        StraightToBankInstructionPriority instructionPriority = StraightToBankInstructionPriority.fromString(batch.getInstructionPriority());
        StraightToBankChargeBearer chargeBearer = StraightToBankChargeBearer.fromString(batch.getChargeBearer());

        return StraightToBankBatch.builder()
                .messageId(batch.getMessageId())
                .messageIdSeq(batch.getMessageIdSeq())
                .schemeId(batch.getSchemeId())
                .schemeName(batch.getSchemeName())
                .schemeCode(batch.getSchemeCode())
                .countryCode(batch.getCountryCode())
                .baseCurrencyCode(batch.getBaseCurrencyCode())
                .batchDate(new Date())
                .preparedById(batch.getPreparedById())
                .preparedByName(batch.getPreparedByName())
                .cashbookId(batch.getCashbookId())
                .cashbookName(batch.getCashbookName())
                .debitAccountName(batch.getCashbookAccountName())
                .debitAccountNo(batch.getCashbookAccountNo())
                .cashbookSwiftCode(batch.getCashbookSwiftCode())
                .countTransactions(batch.getCountLines())
                .total(batch.getTotalSourceCurrency())
                .totalBc(batch.getTotalBaseCurrency())
                .consolidatedPosting(consolidateLines)
                .instructionPriority(instructionPriority)
                .valueDate(DateUtil.convertStringToDate(batch.getValueDate()))
                .chargeBearer(chargeBearer)
                .batchTitle(batch.getBatchTitle())
                .build();

    }

    public StraightToBankBatchLine convertLineDtoToEntity(StraightToBankPayloadLineDto line){

        return StraightToBankBatchLine.builder()
                .paymentId(line.getPaymentId())
                .batchPaymentScheduleId(line.getBatchPaymentScheduleId())
                .customerRef(line.getCustomerRef())
                .amountSc(line.getAmountSc())
                .amountBc(line.getAmountBc())
                .spotRate(line.getSpotRate())
                .currencyId(line.getCurrencyId())
                .paymentCurrencyCode(line.getPaymentCurrencyCode())
                .baseCurrencyCode(line.getBaseCurrencyCode())
                .purposeOfPayment(line.getPurposeOfPayment())
                .forexType(line.getForexType())
                .forexDealNo(line.getForexDealNo())
                .forexDealerName(line.getForexDealerName())
                .forexDirectInverse(line.getForexDirectInverse())
                .maturityDate(line.getMaturityDate()!=null? DateUtil.convertStringToDate(line.getMaturityDate()):null)
                .intermediaryBankCode(line.getIntermediaryBankCode())
                .localBankCode(line.getLocalBankCode())
                .bankCode(line.getBankCode())
                .branchCode(line.getBranchCode())
                .bankName(line.getBankName())
                .swiftCode(line.getSwiftCode())
                .bankCountryCode(line.getBankCountryCode())
                .accountName(line.getAccountName())
                .accountNo(line.getAccountNo())
                .beneficiaryName(line.getBeneficiaryName())
                .email(line.getEmail())
                .particulars(line.getParticulars())
                .paymentType(line.getPaymentType())
                .refCode(line.getRefCode())
                .transactionCurrency(line.getTransactionCurrency())
                .beneficiaryAddress(line.getBeneficiaryAddress())
                .beneficiaryCountryCode(line.getBeneficiaryCountryCode())
                .beneficiaryCountryName(line.getBeneficiaryCountryName())
                .instructionGroup(line.getInstructionGroup())
                .build();

    }

    public String deleteByIds(ProcessIDsDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        List<Long> ids = request.getIds();

        BasicValidationUtil.validateIdentifiersAndThrowCustomEx(ids);

        List<StraightToBankBatch> list = batchRepository.findAllById(ids);

        if(list.isEmpty()){
            throw new CustomException(CustomErrorCode.LIST_404, "No Batches Found");
        }

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        int count = 0;

        for(StraightToBankBatch batch : list){

            batchRepository.deleteById(batch.getId());
            count++;

        }

        return count+" Batch(es) Deleted Successfully";

    }

    public List<StraightToBankBatchResponseDto> getList(String dateFromStr, String dateToStr, String searchKey, String cashbookCode){

        Date dateFrom = DateUtil.convertStringToDate(dateFromStr);
        Date dateTo = DateUtil.convertStringToDate(dateToStr);

        List<StraightToBankBatch> batches = batchRepository.findAll(
                StraightToBankBatchSpecifications.filter(dateFrom, dateTo, searchKey, cashbookCode),
                Sort.by(Sort.Direction.DESC, "batchDate")
        );

        List<StraightToBankBatchResponseDto> list =  batches.stream()
                .map(this::convertEntityToResponseDto)
                .toList();

        return list;

    }

    public List<StraightToBankLineResponseDto> getBatchLines(Long batchId){

        List<StraightToBankBatchLine> lines = bankBatchLineRepository.getStraightToBankBatchLineByBatchIdOrderByCustomerRefAsc(batchId);

        List<StraightToBankLineResponseDto> list =  lines.stream()
                .map(this::convertLineEntityToResponseDto)
                .toList();

        return list;

    }

}