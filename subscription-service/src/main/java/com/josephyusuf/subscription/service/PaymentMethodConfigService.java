package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.PaymentMethodConfigDto;
import com.josephyusuf.subscription.entity.PaymentMethodConfig;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.repository.PaymentMethodConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodConfigService {

    private final PaymentMethodConfigRepository repository;

    public List<PaymentMethodConfigDto> getAll() {
        return repository.findAll().stream()
                .map(c -> PaymentMethodConfigDto.builder()
                        .provider(c.getProvider())
                        .enabled(c.isEnabled())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();
    }

    public boolean isEnabled(PaymentProvider provider) {
        return repository.findById(provider)
                .map(PaymentMethodConfig::isEnabled)
                .orElse(false);
    }

    @Transactional
    public PaymentMethodConfigDto toggle(PaymentProvider provider) {
        PaymentMethodConfig config = repository.findById(provider)
                .orElseGet(() -> PaymentMethodConfig.builder()
                        .provider(provider)
                        .enabled(false)
                        .build());
        config.setEnabled(!config.isEnabled());
        config = repository.save(config);
        return PaymentMethodConfigDto.builder()
                .provider(config.getProvider())
                .enabled(config.isEnabled())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
