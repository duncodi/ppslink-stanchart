package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.UserDto;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailBatchRequestDto;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailRequestDto;
import com.duncodi.ppslink.stanchart.enums.CrudOperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final RestTemplate restTemplate;

    @Value("${admin.service.url}")
    private String adminServiceUrl;

    public AuditTrailRequestDto buildAuditTrailRequest(CrudOperationType crudOperationType, Long identifier, String objectDescription,
                                                       String previousJson, String currentJson, UserDto user,
                                                       String ipAddress) {

        return AuditTrailRequestDto.builder()
                .crudOperationType(crudOperationType)
                .identifier(identifier)
                .objectDescription(objectDescription)
                .previousJson(previousJson)
                .currentJson(currentJson)
                .userId(user.getId())
                .username(user.getName())
                .ipAddress(ipAddress)
                .build();

    }

    public void sendAuditTrail(List<AuditTrailRequestDto> list, String accessToken) {
        if (list == null) {
            list = new ArrayList<>();
        }

        AuditTrailBatchRequestDto batchRequest = new AuditTrailBatchRequestDto();
        batchRequest.setList(list);

        try {

            String url = adminServiceUrl+"/audit-trail";

            log.info("audit trail posting to "+url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<AuditTrailBatchRequestDto> entity = new HttpEntity<>(batchRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class
            );

            String body = response.getBody();

            //log.info("Audit trail saved: {}", body);

        } catch (Exception e) {
            log.error("Failed to send audit trail: {}", e.getMessage(), e);
        }
    }

    public void buildAndSendSingleTrail(CrudOperationType crudOperationType, Long identifier,
                                        String objectDescription, String previousJson, String currentJson,
                                        UserDto user, String ipAddress, String accessToken) {

        AuditTrailRequestDto auditTrailRequest = this.buildAuditTrailRequest(crudOperationType, identifier,
                objectDescription, previousJson, currentJson, user, ipAddress);

        List<AuditTrailRequestDto> list = new ArrayList<>();
        list.add(auditTrailRequest);

        this.sendAuditTrail(list, accessToken);

    }

}
