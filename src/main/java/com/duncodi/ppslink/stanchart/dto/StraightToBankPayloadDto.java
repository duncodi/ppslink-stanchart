package com.duncodi.ppslink.stanchart.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StraightToBankPayloadDto {

    private String valueDate;

    private Long schemeId;

    private String schemeCode;

    private String countryCode;

    private String baseCurrencyCode;

    private String schemeName;

    private Long preparedById;

    private String preparedByName;

    private String preparerInitials;

    private Integer countLines;

    private BigDecimal totalSourceCurrency;

    private BigDecimal totalBaseCurrency;

    private String cashbookCurrencyCode;

    private String cashbookName;

    private String cashbookAccountNo;

    private String debitAccountTown;

    private String cashbookAccountName;

    private String cashbookBranchCode;

    private String cashbookBankCode;

    private String cashbookBankName;

    private String cashbookSwiftCode;

    private Long cashbookId;

    private String batchTitle;

    private String instructionPriority;

    private String consolidatePaymentLines;

    private String chargeBearer;

    private Long messageIdSeq;

    private String messageId;

    @Builder.Default
    private List<StraightToBankPayloadLineDto> lines = new ArrayList<>();

}
