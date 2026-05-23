package com.josephyusuf.auth.repository;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByPlan(Plan plan);

    long countByEnabled(boolean enabled);

    List<User> findByPlanAndEnabledTrueAndCreatedAtBetween(Plan plan, Instant from, Instant to);

    List<User> findByInTrialTrueAndTrialEndsAtBefore(LocalDateTime dateTime);

    List<User> findByInTrialTrueAndTrialEndsAtBetween(LocalDateTime from, LocalDateTime to);
}
