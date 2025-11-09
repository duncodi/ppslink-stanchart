package com.duncodi.ppslink.stanchart.repository;

import com.duncodi.ppslink.stanchart.model.StraightToBankBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface StraightToBankBatchRepository extends JpaRepository<StraightToBankBatch, Long>, JpaSpecificationExecutor<StraightToBankBatch> {

    @Query(value = "SELECT coalesce(max(message_id_seq), 0) FROM straight_to_bank_batch", nativeQuery = true)
    Long findMaxMessageIdSeqNative();

}
