package com.josephyusuf.subscription.repository;

import com.josephyusuf.subscription.entity.PaymentMethodConfig;
import com.josephyusuf.subscription.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, PaymentProvider> {
}
