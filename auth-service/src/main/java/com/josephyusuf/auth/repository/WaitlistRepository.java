package com.josephyusuf.auth.repository;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    Optional<WaitlistEntry> findByEmailAndPlanTier(String email, Plan planTier);

    List<WaitlistEntry> findAllByNotifiedFalse();

    long countByPlanTier(Plan planTier);
}
