package com.josephyusuf.subscription.dto;

import lombok.*;

/**
 * Réponse retournée au frontend après création d'une transaction Paddle côté serveur.
 * Le front l'utilise pour ouvrir Paddle.js :
 *   Paddle.Checkout.open({ transactionId, customer: { email } })
 *
 * Cf. https://developer.paddle.com/paddlejs/methods/paddle-checkout-open
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaddleCheckoutResponse {

    /** Paddle transaction id, format txn_*. */
    private String transactionId;

    /** Statut renvoyé par Paddle (généralement "ready"). */
    private String status;

    /** URL hostée Paddle (fallback si Paddle.js indisponible). */
    private String checkoutUrl;
}
