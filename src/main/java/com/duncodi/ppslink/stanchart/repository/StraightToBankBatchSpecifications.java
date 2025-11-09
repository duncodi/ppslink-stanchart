package com.duncodi.ppslink.stanchart.repository;

import com.duncodi.ppslink.stanchart.model.StraightToBankBatch;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StraightToBankBatchSpecifications {

    public static Specification<StraightToBankBatch> filter(
            Date dateFrom,
            Date dateTo,
            String searchKey,
            String cashbookCode
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dateFrom != null && dateTo != null) {
                predicates.add(cb.between(root.get("batchDate"), dateFrom, dateTo));
            } else if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("batchDate"), dateFrom));
            } else if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("batchDate"), dateTo));
            }

            if (cashbookCode != null && !cashbookCode.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("cashbookName"), cashbookCode));
            }

            if (searchKey != null && !searchKey.trim().isEmpty()) {
                String likePattern = "%" + searchKey.toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("schemeName")), likePattern),
                                cb.like(cb.lower(root.get("schemeCode")), likePattern),
                                cb.like(cb.lower(root.get("batchTitle")), likePattern),
                                cb.like(cb.lower(root.get("messageId")), likePattern),
                                cb.like(cb.lower(root.get("preparedByName")), likePattern),
                                cb.like(cb.lower(root.get("cashbookName")), likePattern),
                                cb.like(cb.lower(root.get("debitAccountName")), likePattern),
                                cb.like(cb.lower(root.get("debitAccountNo")), likePattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

    }

}
