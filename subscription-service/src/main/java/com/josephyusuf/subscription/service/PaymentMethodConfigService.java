package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.PaymentMethodConfigDto;
import com.josephyusuf.subscription.entity.PaymentMethodConfig;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.repository.PaymentMethodConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodConfigService {

    /**
     * Au-delà de cet ordre, les providers sont considérés comme des agrégateurs
     * internes (PAYTECH, PAYDUNYA) — jamais affichés aux utilisateurs finaux.
     */
    private static final int AGGREGATOR_ORDER_THRESHOLD = 97;

    private final PaymentMethodConfigRepository repository;

    /**
     * Liste tous les moyens de paiement (utilisé côté dashboard admin).
     */
    public List<PaymentMethodConfigDto> getAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(PaymentMethodConfig::getDisplayOrder))
                .map(this::toDto)
                .toList();
    }

    /**
     * Liste les moyens de paiement à afficher aux utilisateurs : enabled = true
     * et hors agrégateurs internes (PAYTECH, PAYDUNYA).
     */
    public List<PaymentMethodConfigDto> getAvailableForClient() {
        return repository.findAll().stream()
                .filter(PaymentMethodConfig::isEnabled)
                .filter(c -> c.getDisplayOrder() <= AGGREGATOR_ORDER_THRESHOLD)
                .sorted(Comparator.comparingInt(PaymentMethodConfig::getDisplayOrder))
                .map(this::toDto)
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
                        .displayOrder(99)
                        .build());
        config.setEnabled(!config.isEnabled());
        config = repository.save(config);
        return toDto(config);
    }

    private PaymentMethodConfigDto toDto(PaymentMethodConfig c) {
        return PaymentMethodConfigDto.builder()
                .provider(c.getProvider())
                .enabled(c.isEnabled())
                .displayName(c.getDisplayName())
                .displayOrder(c.getDisplayOrder())
                .paytechMethodCode(c.getPaytechMethodCode())
                .routing(c.getRouting())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
