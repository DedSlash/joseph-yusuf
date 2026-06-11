package com.josephyusuf.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayTechRequest {

    @NotNull(message = "Le plan est obligatoire")
    private String planTier;

    private String couponCode;

    /**
     * Code PayTech du moyen de paiement à pré-sélectionner sur la page checkout
     * (wave, orange_money, free_money, card). Null = l'utilisateur choisit sur PayTech.
     */
    private String paytechMethodCode;
}
