package com.duncodi.ppslink.stanchart.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StraightToBankResultsDto {

    private String filePath;
    private String status;
    private String statusCode;
    private String fileName;
    private String deliveryResponse;
    private String messageId;
    private Long batchId;
    private String fullResult;

}
