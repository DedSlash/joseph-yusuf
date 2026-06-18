package com.josephyusuf.income.repository;

import com.josephyusuf.income.entity.IncomeSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncomeSourceRepository extends JpaRepository<IncomeSource, UUID> {

    List<IncomeSource> findByUserIdAndActiveTrue(UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);

    long countByUserId(UUID userId);
}
