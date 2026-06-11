package com.josephyusuf.subscription.enums;

public enum RenewalReminderType {
    /** Rappel envoyé 3 jours avant expiration. */
    J_MINUS_3,
    /** Rappel envoyé 1 jour avant expiration. */
    J_MINUS_1,
    /** Email envoyé à l'expiration : abonnement EXPIRED, plan conservé. */
    EXPIRED
}
