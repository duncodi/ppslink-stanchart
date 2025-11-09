package com.duncodi.ppslink.stanchart.dto.audittrail;

import com.duncodi.ppslink.stanchart.enums.CrudOperationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AuditTrailRequestDto {

    private Long identifier;
    private String objectDescription;
    private Long userId;
    private String username;
    private String ipAddress;
    private CrudOperationType crudOperationType;
    private String serviceName;
    private String previousJson;
    private String currentJson;

}
