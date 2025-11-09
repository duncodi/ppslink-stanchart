package com.duncodi.ppslink.stanchart.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessIDsDto {

    private String accessToken;
    private String ipAddress;

    @Builder.Default
    private List<Long> ids = new ArrayList<>();

}
