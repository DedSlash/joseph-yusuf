package com.josephyusuf.subscription.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * Nombre de mois d'abonnement payés en une fois (1-12). Mobile money n'a
     * pas de renouvellement natif : l'utilisateur peut prendre plusieurs mois
     * d'avance. Null traité comme 1.
     */
    @Min(value = 1, message = "Minimum 1 mois")
    @Max(value = 12, message = "Maximum 12 mois")
    private Integer monthsCount;
}
