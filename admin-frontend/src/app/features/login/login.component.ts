import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminAuthService } from '../../core/auth/admin-auth.service';

@Component({
  selector: 'admin-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-shell">
      <div class="login-card">
        <h1>Admin Console</h1>
        <p class="subtitle">Accès réservé aux administrateurs Joseph · Yusuf</p>

        <div *ngIf="errorMessage()" class="alert error">{{ errorMessage() }}</div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-row">
            <label for="email">Email</label>
            <input id="email" class="input" type="email" formControlName="email"
                   autocomplete="username" placeholder="admin@josephyusuf.com" />
            <small *ngIf="emailInvalid()" class="error-text">Email invalide</small>
          </div>

          <div class="form-row">
            <label for="password">Mot de passe</label>
            <input id="password" class="input" type="password" formControlName="password"
                   autocomplete="current-password" />
            <small *ngIf="passwordInvalid()" class="error-text">Mot de passe requis</small>
          </div>

          <button type="submit" class="btn btn-primary"
                  [disabled]="form.invalid || loading()"
                  style="width: 100%; margin-top: 0.5rem;">
            {{ loading() ? 'Connexion…' : 'Se connecter' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .error-text {
      color: var(--status-error);
      font-size: 0.75rem;
      display: block;
      margin-top: 0.3rem;
    }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  protected emailInvalid(): boolean {
    const c = this.form.controls.email;
    return c.touched && c.invalid;
  }

  protected passwordInvalid(): boolean {
    const c = this.form.controls.password;
    return c.touched && c.invalid;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse | Error) => {
        this.loading.set(false);
        this.errorMessage.set(this.resolveError(err));
      }
    });
  }

  private resolveError(err: HttpErrorResponse | Error): string {
    if (err instanceof Error && err.message.includes('ADMIN')) {
      return err.message;
    }
    if (err instanceof HttpErrorResponse) {
      if (err.status === 401) return 'Email ou mot de passe invalide';
      if (err.status === 0) return 'Serveur injoignable';
      return err.error?.message ?? 'Erreur lors de la connexion';
    }
    return 'Erreur inattendue';
  }
}
