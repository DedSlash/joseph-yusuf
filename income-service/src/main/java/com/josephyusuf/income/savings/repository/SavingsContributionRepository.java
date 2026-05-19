package com.josephyusuf.income.savings.repository;

import com.josephyusuf.income.savings.entity.SavingsContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SavingsContributionRepository extends JpaRepository<SavingsContribution, UUID> {

    Page<SavingsContribution> findByGoalIdAndUserIdOrderByYearDescMonthDescCreatedAtDesc(
            UUID goalId, UUID userId, Pageable pageable);

    List<SavingsContribution> findByGoalIdAndUserIdAndMonthAndYear(
            UUID goalId, UUID userId, int month, int year);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM SavingsContribution c " +
            "WHERE c.goalId = :goalId")
    BigDecimal sumByGoalId(@Param("goalId") UUID goalId);

    void deleteByGoalId(UUID goalId);
}
