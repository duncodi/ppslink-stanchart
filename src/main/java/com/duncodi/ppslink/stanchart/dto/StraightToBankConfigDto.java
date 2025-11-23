package com.duncodi.ppslink.stanchart.dto;

import com.duncodi.ppslink.stanchart.enums.YesNo;
import com.duncodi.ppslink.stanchart.util.RoundingType;
import jakarta.persistence.Column;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StraightToBankConfigDto {

    private Long id;
    private String activationKey;
    private String activationUrl;
    private String apiUrl;
    private YesNo urlActivated;
    private String dateUrlActivated;

    private String paymentInitiationEndPoint;
    private String paymentStatusEndPoint;

    private String jwtIssuer;
    private String jwtAudience;
    private String webhookUrl;
    private YesNo enableWebhook;

    private Double jwtExpiryMinutes;
    private Integer jwtTimeoutSeconds;

    private String schemeCode;
    private YesNo activateIntegration;
    private YesNo promoteToProduction;
    private YesNo replaceSpecialChars;
    private RoundingType roundPaymentsTo;
    private YesNo truncateTrailingDecimals;

}
