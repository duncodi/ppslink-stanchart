package com.duncodi.ppslink.stanchart.repository;

import com.duncodi.ppslink.stanchart.model.StraightToBankBatchLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StraightToBankBatchLineRepository extends JpaRepository<StraightToBankBatchLine, Long> {

    List<StraightToBankBatchLine> getStraightToBankBatchLineByBatchIdOrderByCustomerRefAsc(Long batchId);

}
