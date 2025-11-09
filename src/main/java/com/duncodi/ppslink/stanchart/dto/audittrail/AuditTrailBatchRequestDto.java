package com.duncodi.ppslink.stanchart.dto.audittrail;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AuditTrailBatchRequestDto {

    private final String serviceName = "Standard Chartered Service";

    @Builder.Default
    private List<AuditTrailRequestDto> list = new ArrayList<>();

}
