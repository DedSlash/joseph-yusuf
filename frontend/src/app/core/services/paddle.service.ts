import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Wrapper Paddle.js v2 (https://developer.paddle.com/paddlejs/overview).
 *
 * - Paddle.js est chargé statiquement depuis index.html
 *   (`<script src="https://cdn.paddle.com/paddle/v2/paddle.js">`).
 * - `Initialize` est idempotent : on protège quand même avec `initialized`.
 * - `openCheckout` reçoit une transaction déjà créée côté serveur (txn_*)
 *   pour que la logique métier (prix, discount, custom_data) reste autoritaire
 *   côté backend.
 */
@Injectable({ providedIn: 'root' })
export class PaddleService {
  private initialized = false;

  private get paddle(): any {
    return (window as any).Paddle;
  }

  init(): void {
    if (this.initialized) return;
    if (!this.paddle) {
      throw new Error('Paddle.js indisponible (script non chargé)');
    }
    if (environment.paddleEnvironment === 'sandbox') {
      this.paddle.Environment.set('sandbox');
    }
    this.paddle.Initialize({ token: environment.paddleClientToken });
    this.initialized = true;
  }

  openCheckout(transactionId: string, email: string): void {
    this.init();
    this.paddle.Checkout.open({
      transactionId,
      customer: { email },
      settings: {
        displayMode: 'overlay',
        theme: 'dark',
        locale: 'fr',
        successUrl: window.location.origin + '/subscription/success'
      }
    });
  }
}
