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

    @Column(name = "customer_ref")
    private String customerRef;

    @Column(name = "amount_sc")
    private BigDecimal amountSc;

    @Column(name = "amount_bc")
    private BigDecimal amountBc;

    @Column(name = "spot_rate")
    private Double spotRate;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "payment_currency_code")
    private String paymentCurrencyCode;

    @Column(name = "purpose_of_payment")
    private String purposeOfPayment;

    @Column(name = "forex_type")
    private String forexType;

    @Column(name = "forex_deal_no")
    private String forexDealNo;

    @Column(name = "forex_dealer_name")
    private String forexDealerName;

    @Column(name = "forext_direct_or_inverse")
    private String forexDirectInverse;

    @Column(name = "maturity_date")
    private Date maturityDate;

    @Column(name = "intermediary_bank_code")
    private String intermediaryBankCode;

    @Column(name = "local_bank_code")
    private String localBankCode;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "swift_code")
    private String swiftCode;

    @Column(name = "bank_country_code")
    private String bankCountryCode;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "email")
    private String email;

    @Column
    private String particulars;

    @Column(name = "debit_account_no")
    private String debitAccountNo;

    @Column(name = "debit_account_name")
    private String debitAccountName;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "ref_code")
    private String refCode;

    @Column(name = "transaction_currency")
    private String transactionCurrency;

    @Column(name = "beneficiary_address")
    private String beneficiaryAddress;

    @Column(name = "beneficiary_country_code")
    private String beneficiaryCountryCode;

    @Column(name = "beneficiary_country_name")
    private String beneficiaryCountryName;

    @Column(name = "json_request", length = 65535)
    @Lob
    private String jsonRequest;

    @Column(name = "recall_initiated_by_id")
    private Long recallInitiatedById;

    @Column(name = "recall_initiated_by_name")
    private String recallInitiatedByName;

    @Column(name = "recall_initiated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallInitiatedDate;

    @Column(name = "recall_certified_by_id")
    private Long recallCertifiedById;

    @Column(name = "recall_certified_by_name")
    private String recallCertifiedByName;

    @Column(name = "recall_certified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallCertifiedDate;

    @Column(name = "recall_approved_by_id")
    private Long recallApprovedById;

    @Column(name = "recall_approved_by_name")
    private String recallApprovedByName;

    @Column(name = "recall_approved_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date recallApprovedDate;

    @Column
    private String comments;

    @Column(name = "old_payment_id")
    private Long oldPaymentId;

    @Column(name = "instruction_group")
    private String instructionGroup;

}
