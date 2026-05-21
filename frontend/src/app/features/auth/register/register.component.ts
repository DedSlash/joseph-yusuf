import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { RegisterRequest } from '../../../shared/models/user.model';

interface RegisterForm {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  promoCode: string;
}

@Component({
  selector: 'app-register',
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
          <h2 class="form-title">Creer un compte</h2>
          <p class="form-subtitle">Commencez a gerer vos revenus intelligemment</p>

          <div class="promo-banner promo-valid" *ngIf="urlPromoValid">
            Code promo {{ urlPromoCode }} detecte — la reduction sera appliquee
            automatiquement lors de votre abonnement !
          </div>
          <div class="promo-banner promo-invalid" *ngIf="urlPromoInvalid">
            Le code promo {{ urlPromoCode }} n'est plus valide ou a expire.
          </div>

          <div class="error-message" *ngIf="errorMessage">
            {{ errorMessage }}
          </div>

          <form (ngSubmit)="onSubmit()" #registerForm="ngForm">
            <div class="form-row">
              <div class="form-group">
                <label for="firstName">Prenom</label>
                <input
                  type="text"
                  id="firstName"
                  name="firstName"
                  [(ngModel)]="form.firstName"
                  required
                  placeholder="Joseph"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="lastName">Nom</label>
                <input
                  type="text"
                  id="lastName"
                  name="lastName"
                  [(ngModel)]="form.lastName"
                  required
                  placeholder="Yusuf"
                  class="form-input"
                />
              </div>
            </div>

            <div class="form-group">
              <label for="email">Email</label>
              <input
                type="email"
                id="email"
                name="email"
                [(ngModel)]="form.email"
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
                [(ngModel)]="form.password"
                required
                minlength="6"
                placeholder="••••••••"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="confirmPassword">Confirmer le mot de passe</label>
              <input
                type="password"
                id="confirmPassword"
                name="confirmPassword"
                [(ngModel)]="form.confirmPassword"
                required
                placeholder="••••••••"
                class="form-input"
              />
            </div>

            <div class="form-group promo-group">
              <button type="button" class="promo-toggle-link" (click)="showPromo = !showPromo">
                {{ showPromo ? '− Masquer' : '+ Vous avez un code promo ?' }}
              </button>
              <div *ngIf="showPromo" class="promo-input-wrapper">
                <input
                  type="text"
                  id="promoCode"
                  name="promoCode"
                  [(ngModel)]="form.promoCode"
                  placeholder="CODE-PROMO"
                  class="form-input promo-input"
                  style="text-transform:uppercase"
                />
                <p class="promo-hint">Le code sera validé après inscription et appliqué à votre premier abonnement.</p>
              </div>
            </div>

            <button type="submit" class="btn-submit" [disabled]="loading">
              {{ loading ? 'Creation...' : 'Creer mon compte' }}
            </button>
          </form>

          <p class="form-footer">
            Deja un compte ?
            <a routerLink="/login" class="link-gold">Se connecter</a>
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
      background: #0D0B07;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem;
    }

    .branding {
      max-width: 400px;
      text-align: center;
    }

    .logo {
      font-family: 'Cormorant Garamond', serif;
      font-size: 2.5rem;
      font-weight: 600;
      color: #C9A84C;
      margin-bottom: 1rem;
    }

    .tagline {
      font-size: 1.1rem;
      color: #F0E8D0;
      opacity: 0.8;
      margin-bottom: 2rem;
    }

    .verse {
      font-style: italic;
      color: #F0E8D0;
      opacity: 0.6;
      font-size: 0.9rem;
      line-height: 1.6;
      border-left: 2px solid rgba(201, 168, 76, 0.4);
      padding-left: 1rem;
      text-align: left;
    }

    .verse cite {
      display: block;
      margin-top: 0.75rem;
      font-style: normal;
      font-weight: 600;
      color: #C9A84C;
      opacity: 0.8;
    }

    .auth-right {
      flex: 1;
      background: #1A1710;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem;
    }

    .form-wrapper {
      width: 100%;
      max-width: 420px;
    }

    .form-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.8rem;
      color: #F0E8D0;
      margin-bottom: 0.5rem;
    }

    .form-subtitle {
      color: #F0E8D0;
      opacity: 0.6;
      font-size: 0.9rem;
      margin-bottom: 2rem;
    }

    .error-message {
      background: rgba(220, 53, 69, 0.1);
      border: 1px solid rgba(220, 53, 69, 0.3);
      color: #ff6b7a;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      font-size: 0.85rem;
      margin-bottom: 1.5rem;
    }

    .form-row {
      display: flex;
      gap: 1rem;
    }

    .form-row .form-group {
      flex: 1;
    }

    .form-group {
      margin-bottom: 1.25rem;
    }

    .form-group label {
      display: block;
      color: #F0E8D0;
      font-size: 0.85rem;
      font-weight: 500;
      margin-bottom: 0.5rem;
      opacity: 0.8;
    }

    .form-input {
      width: 100%;
      padding: 0.75rem 1rem;
      background: rgba(13, 11, 7, 0.6);
      border: 1px solid rgba(201, 168, 76, 0.15);
      border-radius: 8px;
      color: #F0E8D0;
      font-size: 0.9rem;
      transition: border-color 0.2s;
      outline: none;
      box-sizing: border-box;
    }

    .form-input:focus {
      border-color: #C9A84C;
    }

    .form-input::placeholder {
      color: rgba(240, 232, 208, 0.3);
    }

    .btn-submit {
      width: 100%;
      padding: 0.85rem;
      background: #C9A84C;
      color: #0D0B07;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s, transform 0.1s;
      margin-top: 0.5rem;
    }

    .btn-submit:hover:not(:disabled) {
      background: #DAC372;
      transform: translateY(-1px);
    }

    .btn-submit:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .form-footer {
      margin-top: 1.5rem;
      text-align: center;
      color: #F0E8D0;
      opacity: 0.7;
      font-size: 0.85rem;
    }

    .link-gold {
      color: #C9A84C;
      text-decoration: none;
      font-weight: 500;
    }

    .link-gold:hover {
      text-decoration: underline;
    }

    .promo-banner {
      padding: 0.75rem 1rem;
      border-radius: 8px;
      font-size: 0.85rem;
      margin-bottom: 1.5rem;
      line-height: 1.4;
    }

    .promo-valid {
      background: rgba(76, 175, 80, 0.1);
      border: 1px solid rgba(76, 175, 80, 0.3);
      color: #81c784;
    }

    .promo-invalid {
      background: rgba(255, 152, 0, 0.1);
      border: 1px solid rgba(255, 152, 0, 0.3);
      color: #ffb74d;
    }

    .promo-group { margin-bottom: 1.25rem; }

    .promo-toggle-link {
      background: transparent;
      border: none;
      color: #C9A84C;
      font-size: 0.8rem;
      cursor: pointer;
      padding: 0;
      text-decoration: underline;
      opacity: 0.8;
    }

    .promo-input-wrapper { margin-top: 0.6rem; }

    .promo-input { letter-spacing: 1px; }

    .promo-hint {
      font-size: 0.72rem;
      color: #F0E8D0;
      opacity: 0.45;
      margin: 0.4rem 0 0;
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

      .form-row {
        flex-direction: column;
        gap: 0;
      }
    }
  `]
})
export class RegisterComponent implements OnInit {
  form: RegisterForm = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    promoCode: ''
  };
  loading = false;
  errorMessage = '';
  showPromo = false;
  urlPromoCode = '';
  urlPromoValid = false;
  urlPromoInvalid = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly subscriptionService: SubscriptionService
  ) {}

  ngOnInit(): void {
    const promo = this.route.snapshot.queryParams['promo'];
    if (promo) {
      this.urlPromoCode = promo.trim().toUpperCase();
      this.form.promoCode = this.urlPromoCode;
      this.showPromo = true;
      this.subscriptionService.validatePromoCodePublic(this.urlPromoCode).subscribe({
        next: res => {
          if (res.valid) {
            this.urlPromoValid = true;
            localStorage.setItem('joseph_promo_code', this.urlPromoCode);
          } else {
            this.urlPromoInvalid = true;
          }
        },
        error: () => { /* Network error — silent, don't block registration */ }
      });
    }
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (!this.form.firstName || !this.form.lastName || !this.form.email || !this.form.password || !this.form.confirmPassword) {
      this.errorMessage = 'Veuillez remplir tous les champs.';
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.form.email)) {
      this.errorMessage = 'Veuillez entrer un email valide.';
      return;
    }

    if (this.form.password.length < 6) {
      this.errorMessage = 'Le mot de passe doit contenir au moins 6 caracteres.';
      return;
    }

    if (this.form.password !== this.form.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;

    const request: RegisterRequest = {
      firstName: this.form.firstName,
      lastName: this.form.lastName,
      email: this.form.email,
      password: this.form.password,
      promoCode: this.form.promoCode.trim() || undefined
    };

    this.authService.register(request).subscribe({
      next: () => {
        const promo = this.form.promoCode.trim();
        if (promo) {
          localStorage.setItem('joseph_promo_code', promo.toUpperCase());
          this.router.navigate(['/subscription'], { queryParams: { promo } });
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          this.errorMessage = 'Un compte existe deja avec cet email.';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez reessayer.';
        }
      }
    });
  }
}
