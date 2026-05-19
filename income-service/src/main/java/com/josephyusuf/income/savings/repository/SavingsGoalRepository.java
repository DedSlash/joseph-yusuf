package com.josephyusuf.income.savings.repository;

import com.josephyusuf.income.savings.entity.SavingsGoal;
import com.josephyusuf.income.savings.entity.SavingsGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

    List<SavingsGoal> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    List<SavingsGoal> findByUserIdAndActiveTrueAndStatus(UUID userId, SavingsGoalStatus status);

    long countByUserIdAndActiveTrue(UUID userId);
}
