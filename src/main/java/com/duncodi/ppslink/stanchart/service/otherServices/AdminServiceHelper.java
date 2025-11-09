package com.duncodi.ppslink.stanchart.service.otherServices;

import com.duncodi.ppslink.stanchart.dto.FiltersRequestDto;
import com.duncodi.ppslink.stanchart.dto.TokenDto;
import com.duncodi.ppslink.stanchart.dto.UserDto;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailResponseDto;
import com.duncodi.ppslink.stanchart.exceptions.CustomErrorCode;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceHelper {

    private final RestTemplate restTemplate;

    private final String serviceName = "Standard Chartered Service";

    @Value("${admin.service.url}")
    private String adminServiceUrl;

    public UserDto getUserFromToken(String accessToken) {

        try {
            String url = adminServiceUrl + "/user/get-basic-details-from-token";
            log.info("Posting to {}", url);

            TokenDto token = TokenDto.builder()
                    .accessToken(accessToken)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<TokenDto> entity = new HttpEntity<>(token, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class
            );

            String body = response.getBody();

            log.info("Response: {}", body);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            // Extract data.id
            JsonNode dataNode = root.path("data");
            long userId = dataNode.path("id").asLong();

            return mapper.readValue(
                    dataNode.toString(),
                    new TypeReference<>() {
                    }
            );

        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new CustomException(e.getMessage());
        }

    }

    public List<AuditTrailResponseDto> getAuditTrails(FiltersRequestDto request) {

        try {

            if(request==null){
                throw new CustomException(CustomErrorCode.REQ_404);
            }

            String dateFrom = request.getDateFrom();
            String dateTo = request.getDateTo();
            String searchKey = request.getSearchKey();
            String accessToken = request.getAccessToken();
            String crudOperationType = request.getCrudOperationType()==null?"":request.getCrudOperationType().name();

            String url = adminServiceUrl
                    + "/audit-trail?serviceName=" + serviceName
                    + "&dateFrom=" + dateFrom
                    + "&dateTo=" + dateTo
                    + "&searchKey=" + searchKey
                    + "&crudOperationType=" + crudOperationType
                    ;

            log.info("Getting from {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String body = response.getBody();
            log.info("Response: {}", body);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            // Extract the "data" array
            JsonNode dataNode = root.get("data");

            // Convert it to List<AuditTrailResponseDto>
            return mapper.readValue(
                    dataNode.toString(),
                    new TypeReference<>() {
                    }
            );

        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new CustomException(e.getMessage());
        }
    }


}
