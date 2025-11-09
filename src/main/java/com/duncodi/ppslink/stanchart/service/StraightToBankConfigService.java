package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StraightToBankConfigService {

    private final RestTemplate restTemplate;

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

        //config = this.validateAndCreateSubfolders(config, throwSshError);

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
                .apiUrl(config.getApiUrl())
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

          /*  auditTrailService.trail(JsonUtil.convertToPrettyJsonString(user), JsonUtil.convertToPrettyJsonString(user), null, ipAddress,
                    CrudOperationType.DELETE, user.getId(), "User Details Deleted", true);
*/
            repository.deleteById(config.getId());

            count++;

        }

        return count+" Configuration(s) of Deleted Successfully";

    }

    public StraightToBankConfigDto processUrlActivation(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        log.info("generating ssh keys....");

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        if(request.getId()==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Identifier Not Found!");
        }

        StraightToBankConfig config = repository.findById(request.getId()).orElse(null);

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found using "+request.getId()+"!");
        }

        String activationUrl = config.getActivationUrl();
        String activationKey = config.getActivationKey();

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

        if(activationUrl==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Activation URL Not Found!");
        }

        if(activationKey==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Activation Key Not Found!");
        }

        activationKey = EncryptDecrypt.decrypt(activationKey);

        log.info("activationUrl>>>>>>>>>>"+activationUrl);
        log.info("activationKey>>>>>>>>>>"+activationKey);

        String previousState = JsonUtil.convertToJsonString(config);

        try{

            String scbToken = standardCharteredTokenGenerator.generateScbJwtToken(config);

            log.info("=== ACTIVATING SCB SERVICE ===");

            log.info("Activation URL: {}", activationUrl);
            log.info("SCB JWT Token: {}", scbToken);

            // Create headers - IMPORTANT: Use the JWT token as Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(scbToken);

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            log.info("Making POST request to SCB activation endpoint...");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getActivationUrl(),
                    entity,
                    String.class
            );

            // Log response details
            log.info("=== SCB ACTIVATION RESPONSE ===");
            log.info("HTTP Status: {}", response.getStatusCode());
            log.info("Response Headers: {}", response.getHeaders());

            String responseBody = response.getBody();
            log.info("Response Body: {}", responseBody);

            config.setUrlActivated(YesNo.YES);
            config.setDateUrlActivated(new Date());

            config = repository.save(config);

            UserDto user = adminServiceHelper.getUserFromToken(JWTUtil.getTokenFromServletRequest(servletRequest));

            String currentState = JsonUtil.convertToJsonString(config);

            auditTrailService.buildAndSendSingleTrail(CrudOperationType.UPDATE, config.getId(),
                    "API URL Activated for Standard Chartered Bank", previousState, currentState,
                    user, ipAddress, accessToken);

        }catch (Exception e){
            e.printStackTrace(System.err);
            throw new CustomException(e.getMessage());
        }

        return this.convertEntityToDto(config);

    }

    @Transactional
    public String performTest(StraightToBankConfigDto request, HttpServletRequest servletRequest){

        log.info("Testing ....");

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        if(request.getId()==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Identifier Not Found!");
        }

        StraightToBankConfig config = repository.findById(request.getId()).orElse(null);

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found using "+request.getId()+"!");
        }

        try{

            String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

            String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

            UserDto user = adminServiceHelper.getUserFromToken(accessToken);

            String currentState = JsonUtil.convertToJsonString(config);

            String res = "Connection Testing Done";

            auditTrailService.buildAndSendSingleTrail(CrudOperationType.UPDATE, config.getId(),
                    res, currentState, currentState, user, ipAddress, accessToken);

            return res;

        }catch (Exception e){
            e.printStackTrace(System.err);
            throw new CustomException(e.getMessage());
        }

    }

}
