package com.josephyusuf.subscription.enums;

/**
 * Durée d'application d'un coupon Stripe :
 * <ul>
 *     <li>{@link #ONCE} : appliqué une seule fois (sur la première invoice)</li>
 *     <li>{@link #FOREVER} : appliqué à vie sur toutes les invoices (ex. EARLY50)</li>
 *     <li>{@link #MONTHS} : appliqué pendant N mois (durée définie côté Stripe)</li>
 * </ul>
 */
public enum CouponDuration {
    ONCE,
    FOREVER,
    MONTHS
}
