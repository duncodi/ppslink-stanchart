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
    private String fileName;
    private Long batchId;

}
