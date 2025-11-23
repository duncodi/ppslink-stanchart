package com.duncodi.ppslink.stanchart.dto;

import com.duncodi.ppslink.stanchart.enums.StraightToBankChargeBearer;
import com.duncodi.ppslink.stanchart.enums.StraightToBankInstructionPriority;
import com.duncodi.ppslink.stanchart.enums.YesNo;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StraightToBankBatchResponseDto {

    private Long id;

    private String batchDate;

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

    private String cashbookAccountName;

    private String cashbookBranchCode;

    private String cashbookBankCode;

    private String cashbookSwiftCode;

    private Long cashbookId;

    private String batchTitle;

    private StraightToBankInstructionPriority instructionPriority;

    private YesNo consolidatePaymentLines;

    private StraightToBankChargeBearer chargeBearer;

    private Long messageIdSeq;

    private String messageId;

    private Long recallInitiatedById;
    private String recallInitiatedByName;
    private String recallInitiatedDate;
    private Long recallCertifiedById;
    private String recallCertifiedByName;
    private String recallCertifiedDate;
    private Long recallApprovedById;
    private String recallApprovedByName;
    private String recallApprovedDate;

    private String decryptedFileName;
    private String encryptedFileName;
    private String fileNameNative;

    private String jsonRequest;
    private String deliveryStatus;
    private String statusCode;
    private String apiResponse;

}
