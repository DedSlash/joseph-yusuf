package com.josephyusuf.admin.repository;

import com.josephyusuf.admin.entity.PromoCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCode(String code);

    boolean existsByCode(String code);

    Page<PromoCode> findAllByActive(boolean active, Pageable pageable);
}
