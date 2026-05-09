package com.josephyusuf.admin.mapper;

import com.josephyusuf.admin.dto.PromoCodeResponse;
import com.josephyusuf.admin.entity.PromoCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromoCodeMapperTest {

    private final PromoCodeMapper mapper = new PromoCodeMapperImpl();

    @Test
    @DisplayName("toResponse - mappe tous les champs")
    void toResponse_full() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        PromoCode entity = PromoCode.builder()
                .id(id)
                .code("JOSEPH20")
                .description("20% off")
                .discountPercent(20)
                .maxUses(100)
                .usedCount(7)
                .expiresAt(now)
                .active(true)
                .createdBy(UUID.randomUUID())
                .createdAt(now)
                .build();

        PromoCodeResponse dto = mapper.toResponse(entity);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getCode()).isEqualTo("JOSEPH20");
        assertThat(dto.getDescription()).isEqualTo("20% off");
        assertThat(dto.getDiscountPercent()).isEqualTo(20);
        assertThat(dto.getMaxUses()).isEqualTo(100);
        assertThat(dto.getUsedCount()).isEqualTo(7);
        assertThat(dto.getExpiresAt()).isEqualTo(now);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toResponse - null in null out")
    void toResponse_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
