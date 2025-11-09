package com.duncodi.ppslink.stanchart.model;

import com.duncodi.ppslink.stanchart.enums.YesNo;
import com.duncodi.ppslink.stanchart.util.RoundingType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sc_straight_to_bank_configs")
public class StraightToBankConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "activation_key", length = 65535)
    @Lob
    private String activationKey;

    @Column(name = "activation_url")
    private String activationUrl;

    @Column(name = "url_activated")
    @Enumerated(EnumType.STRING)
    private YesNo urlActivated;

    @Column(name = "date_url_activated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUrlActivated;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "jwt_issuer")
    private String jwtIssuer;

    @Column(name = "jwt_audience")
    private String jwtAudience;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "enable_webhook")
    @Enumerated(EnumType.STRING)
    private YesNo enableWebhook;

    @Column(name = "jwt_expiry_minutes")
    private Double jwtExpiryMinutes;

    @Column(name = "jwt_timeout_seconds")
    private Integer jwtTimeoutSeconds;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "activate_integration")
    @Enumerated(EnumType.STRING)
    private YesNo activateIntegration;

    @Column(name = "promote_to_production")
    @Enumerated(EnumType.STRING)
    private YesNo promoteToProduction;

    @Column(name = "replace_special_chars")
    @Enumerated(EnumType.STRING)
    private YesNo replaceSpecialChars;

    @Column(name = "replace_payments_to")
    @Enumerated(EnumType.STRING)
    private RoundingType roundPaymentsTo;

    @Column(name = "truncate_trailing_decimals")
    @Enumerated(EnumType.STRING)
    private YesNo truncateTrailingDecimals;

}
