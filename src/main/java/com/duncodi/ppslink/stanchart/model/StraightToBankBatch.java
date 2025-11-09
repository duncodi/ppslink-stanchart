package com.duncodi.ppslink.stanchart.model;

import com.duncodi.ppslink.stanchart.enums.StraightToBankChargeBearer;
import com.duncodi.ppslink.stanchart.enums.StraightToBankInstructionPriority;
import com.duncodi.ppslink.stanchart.enums.YesNo;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sc_straight_to_bank_batch")
public class StraightToBankBatch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "message_id_seq")
    private Long messageIdSeq;

    @Column(name = "scheme_id")
    private Long schemeId;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "batch_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date batchDate;

    @Column(name = "prepared_by_id")
    private Long preparedById;

    @Column(name = "prepared_by_name")
    private String preparedByName;

    @Column(name = "country_code")
    private String countryCode;

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

    @Column(name = "cashbook_id")
    private Long cashbookId;

    @Column(name = "cashbook_name")
    private String cashbookName;

    @Column(name = "debit_account_name")
    private String debitAccountName;

    @Column(name = "debit_account_no")
    private String debitAccountNo;

    @Column(name = "count_transactions")
    private Integer countTransactions;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "total_bc")
    private BigDecimal totalBc;

    @Column(name = "consolidated_posting")
    @Enumerated(EnumType.STRING)
    private YesNo consolidatedPosting;

    @Column(name = "instruction_priority")
    @Enumerated(EnumType.STRING)
    private StraightToBankInstructionPriority instructionPriority;

    @Column(name = "value_date")
    @Temporal(TemporalType.DATE)
    private Date valueDate;

    @Column(name = "charge_bearer")
    @Enumerated(EnumType.STRING)
    private StraightToBankChargeBearer chargeBearer;

    @Column(name = "batch_title")
    private String batchTitle;

    @Column(name = "decrypted_file_name")
    private String decryptedFileName;

    @Column(name = "encrypted_file_name")
    private String encryptedFileName;

    @Column(name = "file_name_native")
    private String fileNameNative;

    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StraightToBankBatchLine> lines = new ArrayList<>();

    public void addLine(StraightToBankBatchLine line) {
        lines = lines==null?new ArrayList<>():lines;
        lines.add(line);
        line.setBatch(this);
    }
    public void addLines(List<StraightToBankBatchLine> lines) {
        for(StraightToBankBatchLine line : lines){
            this.addLine(line);
        }
    }


}
