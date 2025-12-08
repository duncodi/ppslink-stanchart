package com.duncodi.ppslink.stanchart.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sc_straight_to_bank_lines")
public class StraightToBankBatchLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private StraightToBankBatch batch;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "batch_payment_schedule_id")
    private Long batchPaymentScheduleId;

    @Column(name = "customer_ref", length = 50)
    private String customerRef;

    @Column(name = "amount_sc")
    private BigDecimal amountSc;

    @Column(name = "amount_bc")
    private BigDecimal amountBc;

    @Column(name = "spot_rate")
    private Double spotRate;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "payment_currency_code", length = 10)
    private String paymentCurrencyCode;

    @Column(name = "base_currency_code", length = 10)
    private String baseCurrencyCode;

    @Column(name = "purpose_of_payment", length = 100)
    private String purposeOfPayment;

    @Column(name = "forex_type", length = 50)
    private String forexType;

    @Column(name = "forex_deal_no", length = 50)
    private String forexDealNo;

    @Column(name = "forex_dealer_name", length = 150)
    private String forexDealerName;

    @Column(name = "forext_direct_or_inverse", length = 50)
    private String forexDirectInverse;

    @Column(name = "maturity_date")
    private Date maturityDate;

    @Column(name = "intermediary_bank_code", length = 50)
    private String intermediaryBankCode;

    @Column(name = "local_bank_code", length = 25)
    private String localBankCode;

    @Column(name = "bank_code", length = 25)
    private String bankCode;

    @Column(name = "branch_code", length = 25)
    private String branchCode;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "bank_country_code", length = 10)
    private String bankCountryCode;

    @Column(name = "account_name", length = 200)
    private String accountName;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "beneficiary_name", length = 150)
    private String beneficiaryName;

    @Column(name = "email", length = 250)
    private String email;

    @Column(name = "particulars", length = 150)
    private String particulars;

    @Column(name = "debit_account_no", length = 50)
    private String debitAccountNo;

    @Column(name = "debit_account_name", length = 150)
    private String debitAccountName;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "ref_code", length = 50)
    private String refCode;

    @Column(name = "transaction_currency", length = 10)
    private String transactionCurrency;

    @Column(name = "beneficiary_address", length = 200)
    private String beneficiaryAddress;

    @Column(name = "beneficiary_country_code", length = 10)
    private String beneficiaryCountryCode;

    @Column(name = "beneficiary_town", length = 100)
    private String beneficiaryTown;

    @Column(name = "beneficiary_country_name", length = 100)
    private String beneficiaryCountryName;

    @Lob
    @Column(name = "json_request")
    private String jsonRequest;

    @Column(name = "recall_initiated_by_id")
    private Long recallInitiatedById;

    @Column(name = "recall_initiated_by_name", length = 100)
    private String recallInitiatedByName;

    @Column(name = "recall_initiated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallInitiatedDate;

    @Column(name = "recall_certified_by_id")
    private Long recallCertifiedById;

    @Column(name = "recall_certified_by_name", length = 100)
    private String recallCertifiedByName;

    @Column(name = "recall_certified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallCertifiedDate;

    @Column(name = "recall_approved_by_id")
    private Long recallApprovedById;

    @Column(name = "recall_approved_by_name", length = 100)
    private String recallApprovedByName;

    @Column(name = "recall_approved_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallApprovedDate;

    @Column(name = "comments", length = 200)
    private String comments;

    @Column(name = "old_payment_id")
    private Long oldPaymentId;

    @Column(name = "instruction_group", length = 100)
    private String instructionGroup;
}