package com.duncodi.ppslink.stanchart.repository;

import com.duncodi.ppslink.stanchart.model.StraightToBankConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StraightToBankConfigRepository extends JpaRepository<StraightToBankConfig, Long> {

    StraightToBankConfig findStraightToBankConfigBySchemeCode(String schemeCode);

}
