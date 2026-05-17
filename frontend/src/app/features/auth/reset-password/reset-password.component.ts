import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-container">
      <div class="auth-right">
        <div class="form-wrapper">
          <h2 class="form-title">Nouveau mot de passe</h2>
          <p class="form-subtitle">Choisissez un mot de passe (min. 8 caractères).</p>

          <div class="error-message" *ngIf="errorMessage">{{ errorMessage }}</div>
          <div class="success-message" *ngIf="success">
            Mot de passe réinitialisé avec succès. Redirection vers la connexion...
          </div>

          <form *ngIf="!success && token" (ngSubmit)="onSubmit()" #f="ngForm">
            <div class="form-group">
              <label for="newPassword">Nouveau mot de passe</label>
              <input
                type="password" id="newPassword" name="newPassword"
                [(ngModel)]="newPassword" required minlength="8"
                placeholder="••••••••" class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="confirm">Confirmation</label>
              <input
                type="password" id="confirm" name="confirm"
                [(ngModel)]="confirm" required
                placeholder="••••••••" class="form-input"
              />
            </div>

            <button type="submit" class="btn-submit" [disabled]="loading">
              {{ loading ? 'Validation...' : 'Réinitialiser' }}
            </button>
          </form>

          <div *ngIf="!token" class="error-message">
            Lien invalide. Veuillez redemander un email de réinitialisation.
          </div>

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
    .error-message { background:rgba(220,53,69,.1); border:1px solid rgba(220,53,69,.3); color:#ff6b7a; padding:.75rem 1rem; border-radius:8px; font-size:.85rem; margin-bottom:1.5rem; }
    .success-message { background:rgba(40,167,69,.1); border:1px solid rgba(40,167,69,.3); color:#7ad99e; padding:.75rem 1rem; border-radius:8px; font-size:.85rem; margin-bottom:1.5rem; }
  `]
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  newPassword = '';
  confirm = '';
  loading = false;
  errorMessage = '';
  success = false;

  constructor(
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.newPassword.length < 8) {
      this.errorMessage = 'Le mot de passe doit contenir au moins 8 caractères.';
      return;
    }
    if (this.newPassword !== this.confirm) {
      this.errorMessage = 'Les deux mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;
    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Lien invalide ou expiré.';
      }
    });
  }
}
