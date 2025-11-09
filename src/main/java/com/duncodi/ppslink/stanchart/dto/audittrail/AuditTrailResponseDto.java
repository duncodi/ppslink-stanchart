package com.duncodi.ppslink.stanchart.dto.audittrail;

import com.duncodi.ppslink.stanchart.enums.CrudOperationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrailResponseDto {

    private Long id;
    private Long identifier;
    private String objectDescription;
    private String fullChange;
    private String shortChange;
    private Long userId;
    private String username;
    private String ipAddress;
    private CrudOperationType crudOperationType;
    private String serviceName;
    private String previousJson;
    private String currentJson;
    private String activityTime;

}
