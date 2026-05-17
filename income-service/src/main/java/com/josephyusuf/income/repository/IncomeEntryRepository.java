package com.josephyusuf.income.repository;

import com.josephyusuf.income.entity.IncomeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, UUID> {

    List<IncomeEntry> findByUserIdAndMonthAndYear(UUID userId, int month, int year);

    List<IncomeEntry> findByIncomeSourceIdAndUserId(UUID incomeSourceId, UUID userId);

    boolean existsByIncomeSourceIdAndMonthAndYear(UUID incomeSourceId, int month, int year);

    @Query("SELECT COALESCE(SUM(e.amountXof), 0) FROM IncomeEntry e " +
            "WHERE e.userId = :userId AND e.month = :month AND e.year = :year")
    BigDecimal sumByUserIdAndMonthAndYear(@Param("userId") UUID userId,
                                          @Param("month") int month,
                                          @Param("year") int year);

    @Query("SELECT DISTINCT CONCAT(e.year, '-', LPAD(CAST(e.month AS string), 2, '0')) " +
            "FROM IncomeEntry e WHERE e.userId = :userId " +
            "ORDER BY CONCAT(e.year, '-', LPAD(CAST(e.month AS string), 2, '0')) DESC")
    List<String> findDistinctMonthsByUserId(@Param("userId") UUID userId);
}
