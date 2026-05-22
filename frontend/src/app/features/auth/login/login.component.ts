import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginRequest } from '../../../shared/models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-container">
      <div class="auth-left">
        <div class="branding">
          <h1 class="logo">Joseph &middot; Yusuf</h1>
          <p class="tagline">Gerez vos revenus avec sagesse</p>
          <blockquote class="verse">
            &laquo; Qu'ils rassemblent tous les produits de ces bonnes annees qui vont venir ;
            qu'ils fassent des amas de ble [...] et que ces provisions soient en reserve
            pour le pays, pour les sept annees de famine. &raquo;
            <cite>Genese 41:35-36</cite>
          </blockquote>
        </div>
      </div>
      <div class="auth-right">
        <div class="form-wrapper">
          <h2 class="form-title">Connexion</h2>
          <p class="form-subtitle">Accedez a votre espace de gestion</p>

          <div class="error-message" *ngIf="errorMessage">
            {{ errorMessage }}
          </div>

          <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
            <div class="form-group">
              <label for="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                [(ngModel)]="credentials.email"
                required
                email
                placeholder="votre@email.com"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="password">Mot de passe</label>
              <input
                type="password"
                id="password"
                name="password"
                [(ngModel)]="credentials.password"
                required
                minlength="6"
                placeholder="••••••••"
                class="form-input"
              />
            </div>

            <button type="submit" class="btn-submit" [disabled]="loading">
              {{ loading ? 'Connexion...' : 'Se connecter' }}
            </button>
          </form>

          <p class="form-footer-link">
            <a routerLink="/forgot-password" class="link-secondary">Mot de passe oublie ?</a>
          </p>

          <p class="form-footer">
            Pas encore de compte ?
            <a routerLink="/register" class="link-gold">Creer un compte</a>
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex;
      min-height: 100vh;
    }

    .auth-left {
      flex: 1;
      background: var(--night-1);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem;
      position: relative;
      overflow: hidden;
    }

    .auth-left::before {
      content: "";
      position: absolute; inset: 0; pointer-events: none;
      background:
        radial-gradient(600px 400px at 30% 20%, rgba(201, 168, 76, 0.1), transparent 60%),
        radial-gradient(500px 400px at 70% 80%, rgba(93, 173, 226, 0.05), transparent 60%);
    }

    .branding {
      max-width: 400px;
      text-align: center;
      position: relative;
      z-index: 1;
    }

    .logo {
      font-family: var(--font-serif);
      font-size: 2.5rem;
      font-weight: 600;
      color: var(--gold-light);
      margin-bottom: 1rem;
      letter-spacing: -0.02em;
    }

    .tagline {
      font-size: 1.1rem;
      color: var(--text-1);
      margin-bottom: 2rem;
    }

    .verse {
      font-family: var(--font-serif);
      font-style: italic;
      color: var(--text-2);
      font-size: 0.95rem;
      line-height: 1.6;
      border-left: 2px solid var(--line-strong);
      padding-left: 1rem;
      text-align: left;
    }

    .verse cite {
      display: block;
      margin-top: 0.75rem;
      font-style: normal;
      font-weight: 600;
      color: var(--gold);
      font-family: var(--font-sans);
      font-size: 0.8rem;
      letter-spacing: 0.04em;
    }

    .auth-right {
      flex: 1;
      background: var(--night-2);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem;
    }

    .form-wrapper {
      width: 100%;
      max-width: 380px;
    }

    .form-title {
      font-family: var(--font-serif);
      font-size: 1.8rem;
      color: var(--text-0);
      margin-bottom: 0.5rem;
      letter-spacing: -0.01em;
    }

    .form-subtitle {
      color: var(--text-2);
      font-size: 0.9rem;
      margin-bottom: 2rem;
    }

    .error-message {
      background: rgba(231, 76, 60, 0.08);
      border: 1px solid rgba(231, 76, 60, 0.25);
      color: #ff7a6c;
      padding: 0.75rem 1rem;
      border-radius: 10px;
      font-size: 0.85rem;
      margin-bottom: 1.5rem;
    }

    .form-group {
      margin-bottom: 1.25rem;
    }

    .form-group label {
      display: block;
      color: var(--text-2);
      font-size: 12px;
      font-weight: 500;
      margin-bottom: 8px;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    .form-input {
      width: 100%;
      padding: 11px 14px;
      background: rgba(8, 8, 15, 0.5);
      border: 1px solid var(--line-soft);
      border-radius: 10px;
      color: var(--text-0);
      font-size: 14px;
      transition: border-color 0.15s, box-shadow 0.15s;
      outline: none;
      box-sizing: border-box;
    }

    .form-input:focus {
      border-color: var(--gold);
      box-shadow: 0 0 0 3px rgba(201, 168, 76, 0.15);
    }

    .form-input::placeholder {
      color: var(--text-3);
    }

    .btn-submit {
      width: 100%;
      padding: 12px;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      color: #1b1500;
      border: none;
      border-radius: 10px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      transition: transform 0.15s, box-shadow 0.2s;
      margin-top: 0.5rem;
      box-shadow: 0 8px 24px -8px var(--gold-glow);
    }

    .btn-submit:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 12px 32px -8px var(--gold-glow);
    }

    .btn-submit:active:not(:disabled) {
      transform: scale(0.98);
    }

    .btn-submit:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .form-footer {
      margin-top: 1.5rem;
      text-align: center;
      color: var(--text-2);
      font-size: 0.85rem;
    }

    .form-footer-link {
      margin-top: 1rem;
      text-align: center;
      font-size: 0.85rem;
    }

    .link-secondary {
      color: var(--text-2);
      text-decoration: none;
      transition: color 0.15s;
    }

    .link-secondary:hover {
      color: var(--text-0);
    }

    .link-gold {
      color: var(--gold-light);
      text-decoration: none;
      font-weight: 500;
    }

    .link-gold:hover {
      text-decoration: underline;
    }

    @media (max-width: 768px) {
      .auth-container {
        flex-direction: column;
      }

      .auth-left {
        padding: 2rem;
        min-height: auto;
      }

      .verse {
        display: none;
      }
    }
  `]
})
export class LoginComponent {
  credentials: LoginRequest = {
    email: '',
    password: ''
  };
  loading = false;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (!this.credentials.email || !this.credentials.password) {
      this.errorMessage = 'Veuillez remplir tous les champs.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect.';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez reessayer.';
        }
      }
    });
  }
}
