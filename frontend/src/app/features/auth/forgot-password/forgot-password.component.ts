import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-container">
      <div class="auth-right">
        <div class="form-wrapper">
          <h2 class="form-title">Mot de passe oublié</h2>
          <p class="form-subtitle">
            Saisissez votre email — un lien de réinitialisation vous sera envoyé.
          </p>

          <div class="success-message" *ngIf="submitted">
            Si cet email correspond à un compte, vous allez recevoir un lien
            pour réinitialiser votre mot de passe (valide 15 minutes).
          </div>

          <form *ngIf="!submitted" (ngSubmit)="onSubmit()" #f="ngForm">
            <div class="form-group">
              <label for="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                [(ngModel)]="email"
                required
                email
                placeholder="votre@email.com"
                class="form-input"
              />
            </div>

            <button type="submit" class="btn-submit" [disabled]="loading">
              {{ loading ? 'Envoi...' : 'Envoyer le lien' }}
            </button>
          </form>

          <p class="form-footer">
            <a routerLink="/login" class="link-gold">Retour à la connexion</a>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-container { display:flex; min-height:100vh; background:#1A1710; }
    .auth-right { flex:1; display:flex; align-items:center; justify-content:center; padding:3rem; }
    .form-wrapper { width:100%; max-width:380px; }
    .form-title { font-family:'Cormorant Garamond',serif; font-size:1.8rem; color:#F0E8D0; margin-bottom:.5rem; }
    .form-subtitle { color:#F0E8D0; opacity:.6; font-size:.9rem; margin-bottom:2rem; }
    .form-group { margin-bottom:1.25rem; }
    .form-group label { display:block; color:#F0E8D0; font-size:.85rem; font-weight:500; margin-bottom:.5rem; opacity:.8; }
    .form-input { width:100%; padding:.75rem 1rem; background:rgba(13,11,7,.6); border:1px solid rgba(201,168,76,.15); border-radius:8px; color:#F0E8D0; font-size:.9rem; outline:none; box-sizing:border-box; }
    .form-input:focus { border-color:#C9A84C; }
    .btn-submit { width:100%; padding:.85rem; background:#C9A84C; color:#0D0B07; border:none; border-radius:8px; font-size:.95rem; font-weight:600; cursor:pointer; }
    .btn-submit:disabled { opacity:.6; cursor:not-allowed; }
    .form-footer { margin-top:1.5rem; text-align:center; font-size:.85rem; }
    .link-gold { color:#C9A84C; text-decoration:none; font-weight:500; }
    .link-gold:hover { text-decoration:underline; }
    .success-message { background:rgba(40,167,69,.1); border:1px solid rgba(40,167,69,.3); color:#7ad99e; padding:.75rem 1rem; border-radius:8px; font-size:.85rem; margin-bottom:1.5rem; }
  `]
})
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  submitted = false;

  constructor(private authService: AuthService) {}

  onSubmit(): void {
    if (!this.email) return;
    this.loading = true;
    this.authService.forgotPassword(this.email).subscribe({
      next: () => { this.loading = false; this.submitted = true; },
      error: () => { this.loading = false; this.submitted = true; }
    });
  }
}
