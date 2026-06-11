import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { environment } from '../../../environments/environment';

declare let gtag: (...args: any[]) => void;

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private initialized = false;

  constructor(private readonly router: Router) {}

  init(): void {
    if (this.initialized || !environment.gaTrackingId) return;
    this.initialized = true;

    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe((e) => {
      gtag('config', environment.gaTrackingId, { page_path: e.urlAfterRedirects });
    });
  }

  event(action: string, params: Record<string, string | number | boolean> = {}): void {
    if (typeof gtag === 'undefined') return;
    gtag('event', action, params);
  }

  // ── Landing page events ──────────────────────────────────────────

  ctaHeroRegister(): void {
    this.event('cta_hero_register', { event_category: 'landing', event_label: 'hero' });
  }

  ctaPricingFree(): void {
    this.event('cta_pricing_free', { event_category: 'landing', event_label: 'pricing_free' });
  }

  ctaPricingPremium(): void {
    this.event('cta_pricing_premium', { event_category: 'landing', event_label: 'pricing_premium' });
  }

  ctaPricingPremiumPlus(): void {
    this.event('cta_pricing_premium_plus', { event_category: 'landing', event_label: 'pricing_premium_plus' });
  }

  ctaFinalRegister(): void {
    this.event('cta_final_register', { event_category: 'landing', event_label: 'final_cta' });
  }

  navRegister(): void {
    this.event('nav_register', { event_category: 'landing', event_label: 'navbar' });
  }

  // ── Register page events ─────────────────────────────────────────

  registerSubmit(): void {
    this.event('register_submit', { event_category: 'auth', event_label: 'form_submit' });
  }

  promoCodeOpened(): void {
    this.event('promo_code_opened', { event_category: 'auth', event_label: 'promo_toggle' });
  }

  // ── Conversion events ────────────────────────────────────────────

  registrationCompleted(): void {
    this.event('registration_completed', { event_category: 'conversion', event_label: 'signup' });
  }

  trialStarted(): void {
    this.event('trial_started', { event_category: 'conversion', event_label: 'trial_premium_plus' });
  }
}
