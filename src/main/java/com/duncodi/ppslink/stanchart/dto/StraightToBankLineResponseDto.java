package com.duncodi.ppslink.stanchart.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StraightToBankLineResponseDto {

    private Long id;
    private Long paymentId;
    private Long batchPaymentScheduleId;
    private String customerRef;

    private BigDecimal amountSc;
    private BigDecimal amountBc;
    private Double spotRate;
    private Long currencyId;

    private String paymentCurrencyCode;
    private String baseCurrencyCode;
    private String purposeOfPayment;
    private String forexType;
    private String forexDealNo;
    private String forexDealerName;
    private String forexDirectInverse;
    private String maturityDate;

    private String intermediaryBankCode;
    private String localBankCode;
    private String bankCode;
    private String branchCode;
    private String bankName;
    private String swiftCode;
    private String bankCountryCode;

    private String accountName;
    private String accountNo;
    private String beneficiaryName;
    private String email;
    private String particulars;

    private String debitAccountNo;
    private String debitAccountName;

    private String paymentType;
    private String refCode;
    private String transactionCurrency;

    private String beneficiaryAddress;
    private String beneficiaryTown;
    private String beneficiaryCountryCode;
    private String beneficiaryCountryName;

    private String jsonRequest;
    private Long recallInitiatedById;
    private String recallInitiatedByName;
    private String recallInitiatedDate;
    private Long recallCertifiedById;
    private String recallCertifiedByName;
    private String recallCertifiedDate;
    private Long recallApprovedById;
    private String recallApprovedByName;
    private String recallApprovedDate;

    private String comments;
    private Long oldPaymentId;
    private String instructionGroup;

}
