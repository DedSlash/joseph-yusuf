package com.josephyusuf.admin.repository;

import com.josephyusuf.admin.entity.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, UUID> {

    boolean existsByPromoCodeIdAndUserId(UUID promoCodeId, UUID userId);

    long countByPromoCodeId(UUID promoCodeId);
}
