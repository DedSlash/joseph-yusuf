package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.SystemSetting;
import com.josephyusuf.auth.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    public static final String KEY_PAYMENTS_ACTIVE = "payments.active";

    private final SystemSettingRepository repository;

    @Value("${app.payments.active:false}")
    private boolean envPaymentsActive;

    @PostConstruct
    @Transactional
    public void bootstrap() {
        if (repository.findById(KEY_PAYMENTS_ACTIVE).isEmpty()) {
            repository.save(SystemSetting.builder()
                    .key(KEY_PAYMENTS_ACTIVE)
                    .value(Boolean.toString(envPaymentsActive))
                    .build());
            log.info("Réglage {} initialisé depuis l'env à '{}'", KEY_PAYMENTS_ACTIVE, envPaymentsActive);
        }
    }

    @Transactional(readOnly = true)
    public boolean isPaymentsActive() {
        return repository.findById(KEY_PAYMENTS_ACTIVE)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(envPaymentsActive);
    }

    @Transactional
    public boolean setPaymentsActive(boolean active) {
        SystemSetting setting = repository.findById(KEY_PAYMENTS_ACTIVE)
                .orElseGet(() -> SystemSetting.builder().key(KEY_PAYMENTS_ACTIVE).build());
        boolean previous = Boolean.parseBoolean(setting.getValue() == null ? "false" : setting.getValue());
        setting.setValue(Boolean.toString(active));
        repository.save(setting);
        log.info("Réglage {} mis à jour: {} -> {}", KEY_PAYMENTS_ACTIVE, previous, active);
        return previous;
    }
}
