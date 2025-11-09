package com.duncodi.ppslink.stanchart.dto;

import com.duncodi.ppslink.stanchart.enums.CrudOperationType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltersRequestDto {

    private String dateFrom;
    private String dateTo;
    private String searchKey;
    private CrudOperationType crudOperationType;
    private String accessToken;

}
