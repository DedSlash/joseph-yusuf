package com.josephyusuf.income.tips.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Astuce statique de répartition physique de l'argent.
 * Stockée en dur dans le catalogue ; localisée à la volée via {@link MoneyTipsCatalog}.
 * Joseph·Yusuf ne garde pas l'argent du client — ces conseils guident la répartition externe.
 */
@Getter
@Builder
public class MoneyTip {

    private final String id;
    private final String titleFr;
    private final String titleEn;
    private final String descriptionFr;
    private final String descriptionEn;
    private final String icon;
    private final TipMethod method;
    /** Pays où l'astuce s'applique (codes ISO-2). Vide = universel. */
    private final List<String> countries;
    private final String requiredPlan;
    private final String actionUrl;
    private final String actionLabelFr;
    private final String actionLabelEn;
    /** True si l'astuce doit être masquée en mois LEAN (ex : conseils d'investissement). */
    private final boolean hiddenInLean;
    /** True pour les conseils avancés à mettre en avant en mois d'ABUNDANCE. */
    private final boolean priorityInAbundance;
}
