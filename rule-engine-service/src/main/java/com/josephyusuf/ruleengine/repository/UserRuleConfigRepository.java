package com.josephyusuf.ruleengine.repository;

import com.josephyusuf.ruleengine.entity.UserRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRuleConfigRepository extends JpaRepository<UserRuleConfig, UUID> {

    Optional<UserRuleConfig> findByUserId(UUID userId);
}
