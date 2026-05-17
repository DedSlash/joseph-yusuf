package com.josephyusuf.alert.repository;

import com.josephyusuf.alert.entity.Alert;
import com.josephyusuf.alert.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Alert> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    Optional<Alert> findByUserIdAndTypeAndMonthAndYear(UUID userId, AlertType type, Integer month, Integer year);
}
