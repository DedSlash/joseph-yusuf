import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AdminAuthService } from '../../core/auth/admin-auth.service';
import { AdminSupportService } from '../../core/services/admin-support.service';

@Component({
  selector: 'admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="admin-shell">
      <aside class="admin-sidebar">
        <div class="brand">
          🌾 Joseph · Yusuf
          <small>Admin Console</small>
        </div>

        <a class="nav-link" routerLink="/dashboard" routerLinkActive="active">
          <i class="pi pi-chart-line"></i> Dashboard
        </a>
        <a class="nav-link" routerLink="/users" routerLinkActive="active">
          <i class="pi pi-users"></i> Utilisateurs
        </a>
        <a class="nav-link" routerLink="/transactions" routerLinkActive="active">
          <i class="pi pi-credit-card"></i> Transactions
        </a>
        <a class="nav-link" routerLink="/promo-codes" routerLinkActive="active">
          <i class="pi pi-tag"></i> Codes promo
        </a>
        <a class="nav-link" routerLink="/support" routerLinkActive="active">
          <i class="pi pi-question-circle"></i> Support
          <span class="badge-count" *ngIf="openTickets() > 0">{{ openTickets() }}</span>
        </a>
        <a class="nav-link" routerLink="/knowledge-base" routerLinkActive="active">
          <i class="pi pi-book"></i> Base de connaissances
        </a>
        <a class="nav-link" routerLink="/audit-log" routerLinkActive="active">
          <i class="pi pi-history"></i> Audit log
        </a>

        <div class="logout">
          <div class="admin-user" *ngIf="auth.user$ | async as user">
            <div class="email">{{ user.email }}</div>
            <div class="role-badge">ADMIN</div>
          </div>
          <button class="btn btn-ghost" (click)="logout()" style="width: 100%; margin-top: 0.75rem;">
            Déconnexion
          </button>
        </div>
      </aside>

      <main class="admin-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-user {
      padding: 0.75rem;
      border-top: 1px solid var(--border-gold);
      margin-top: 0.5rem;
    }
    .admin-user .email {
      font-size: 0.78rem;
      color: var(--text);
      word-break: break-all;
    }
    .admin-user .role-badge {
      display: inline-block;
      margin-top: 0.4rem;
      font-size: 0.65rem;
      letter-spacing: 0.1em;
      color: var(--gold);
      background: var(--gold-dim);
      padding: 0.15rem 0.5rem;
      border-radius: 1rem;
    }
    .badge-count {
      display: inline-block;
      margin-left: 0.5rem;
      background: var(--gold);
      color: #1a1a1a;
      font-size: 0.7rem;
      font-weight: 600;
      padding: 0.05rem 0.45rem;
      border-radius: 1rem;
      min-width: 1.3rem;
      text-align: center;
    }
  `]
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  protected readonly auth = inject(AdminAuthService);
  private readonly support = inject(AdminSupportService);

  openTickets = signal(0);
  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private static readonly POLL_INTERVAL_MS = 30_000;

  ngOnInit(): void {
    this.refreshCount();
    this.pollHandle = setInterval(() => this.refreshCount(), AdminLayoutComponent.POLL_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  refreshCount(): void {
    this.support.countOpen().subscribe({
      next: n => this.openTickets.set(n),
      error: () => {}
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
