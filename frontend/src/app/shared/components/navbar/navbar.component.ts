import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { DialogModule } from 'primeng/dialog';
import { AuthService } from '../../../core/auth/auth.service';
import { User, Plan } from '../../../shared/models/user.model';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, DialogModule, NotificationBellComponent],
  template: `
    <nav class="navbar" *ngIf="currentUser$ | async as user">
      <div class="navbar-left">
        <a routerLink="/dashboard" class="logo">Joseph &middot; Yusuf</a>
        <div class="nav-links">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">Dashboard</a>
          <a routerLink="/incomes" routerLinkActive="active" class="nav-link">Mes Revenus</a>
        </div>
      </div>
      <div class="navbar-right">
        <app-notification-bell></app-notification-bell>
        <button class="btn-help" (click)="showHelp = true" aria-label="Comment ça marche ?">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14" aria-hidden="true">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/>
          </svg>
          <span>Comment ça marche</span>
        </button>
        <a
          routerLink="/subscription"
          class="btn-upgrade-nav"
          *ngIf="user.plan === 'FREE'"
        >✦ Passer Premium</a>
        <span
          class="plan-badge"
          [ngClass]="getPlanClass(user.plan)"
          *ngIf="user.plan !== 'FREE'"
        >{{ getPlanLabel(user.plan) }}</span>
        <div class="avatar-container" (click)="toggleDropdown()">
          <div class="avatar">{{ getInitials(user) }}</div>
          <div class="dropdown" *ngIf="dropdownOpen">
            <a class="dropdown-item" routerLink="/account">Mon compte</a>
            <a class="dropdown-item" routerLink="/subscription">Mon abonnement</a>
            <button class="dropdown-item" (click)="logout()">Deconnexion</button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Modale d'aide — Principe de Joseph -->
    <p-dialog
      header=" "
      [(visible)]="showHelp"
      [modal]="true"
      [style]="{ width: '600px', maxWidth: '95vw' }"
      [baseZIndex]="2000"
      [draggable]="false"
      [resizable]="false"
      styleClass="help-dialog"
    >
      <div class="help-body">
        <div class="help-icon">✦</div>
        <h2 class="help-title">Le Principe de Joseph</h2>
        <p class="help-intro">
          Joseph · Yusuf s'inspire d'un principe millénaire simple :
          <strong>épargner pendant l'abondance pour tenir pendant la disette</strong>.
          Votre tableau de bord prend vie dès que vous enregistrez vos revenus.
        </p>
        <p class="help-intro" style="margin-top: 0.5rem">
          Il faut au moins <strong>3 mois de données</strong> pour comparer votre mois actuel
          à votre moyenne et déterminer si vous traversez une période d'abondance ou de vaches maigres.
        </p>

        <div class="help-steps">
          <div class="help-step">
            <span class="help-step-num">1</span>
            <div>
              <strong>Saisissez vos revenus du mois</strong>
              <p>Rendez-vous dans « Mes Revenus » pour ajouter votre premier revenu.</p>
            </div>
          </div>
          <div class="help-step">
            <span class="help-step-num">2</span>
            <div>
              <strong>Importez votre historique si vous en avez</strong>
              <p>
                Vous avez déjà des données sur Excel, CSV ou JSON ?
                Importez-les depuis « Mes Revenus » → bouton <em>Importer</em>
                et reprenez votre aventure dans le principe de richesse sans stress.
              </p>
            </div>
          </div>
          <div class="help-step">
            <span class="help-step-num">3</span>
            <div>
              <strong>Laissez Joseph travailler pour vous</strong>
              <p>Après 3 mois, votre tableau de bord vous indique précisément où vous en êtes et comment répartir vos revenus.</p>
            </div>
          </div>
        </div>

        <div class="help-footer">
          <a routerLink="/incomes" (click)="showHelp = false" class="btn-help-start">Commencer maintenant →</a>
        </div>
      </div>
    </p-dialog>
  `,
  styles: [`
    .navbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      height: 64px;
      background: #0D0B07;
      border-bottom: 1px solid rgba(201, 168, 76, 0.2);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 2rem;
      z-index: 1000;
    }

    .navbar-left {
      display: flex;
      align-items: center;
      gap: 2.5rem;
    }

    .logo {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.5rem;
      font-weight: 600;
      color: #C9A84C;
      text-decoration: none;
      letter-spacing: 0.5px;
    }

    .nav-links {
      display: flex;
      gap: 1.5rem;
    }

    .nav-link {
      color: #F0E8D0;
      text-decoration: none;
      font-size: 0.9rem;
      font-weight: 500;
      opacity: 0.7;
      transition: opacity 0.2s, color 0.2s;
    }

    .nav-link:hover,
    .nav-link.active {
      opacity: 1;
      color: #C9A84C;
    }

    .navbar-right {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .plan-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .plan-free {
      background: rgba(128, 128, 128, 0.2);
      color: #aaa;
      border: 1px solid rgba(128, 128, 128, 0.3);
    }

    .plan-premium {
      background: rgba(201, 168, 76, 0.15);
      color: #C9A84C;
      border: 1px solid rgba(201, 168, 76, 0.4);
    }

    .plan-premium-plus {
      background: linear-gradient(135deg, rgba(201, 168, 76, 0.2), rgba(218, 195, 114, 0.2));
      color: #DAC372;
      border: 1px solid rgba(218, 195, 114, 0.5);
    }

    .btn-help {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.3rem 0.75rem;
      background: rgba(201, 168, 76, 0.08);
      border: 1px solid rgba(201, 168, 76, 0.35);
      border-radius: 20px;
      color: #C9A84C;
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      letter-spacing: 0.02em;
      transition: background 0.2s, border-color 0.2s;
      white-space: nowrap;
    }

    .btn-help:hover {
      background: rgba(201, 168, 76, 0.18);
      border-color: rgba(201, 168, 76, 0.6);
    }

    .btn-upgrade-nav {
      padding: 0.3rem 0.85rem;
      background: rgba(201, 168, 76, 0.12);
      border: 1px solid rgba(201, 168, 76, 0.4);
      border-radius: 20px;
      color: #C9A84C;
      font-size: 0.75rem;
      font-weight: 700;
      text-decoration: none;
      transition: background 0.2s;
      white-space: nowrap;
    }

    .btn-upgrade-nav:hover { background: rgba(201, 168, 76, 0.22); }

    .avatar-container {
      position: relative;
      cursor: pointer;
    }

    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: rgba(201, 168, 76, 0.15);
      border: 1px solid rgba(201, 168, 76, 0.4);
      color: #C9A84C;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: #1A1710;
      border: 1px solid rgba(201, 168, 76, 0.2);
      border-radius: 8px;
      min-width: 160px;
      padding: 0.5rem 0;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
    }

    .dropdown-item {
      display: block;
      width: 100%;
      padding: 0.6rem 1rem;
      color: #F0E8D0;
      text-decoration: none;
      font-size: 0.85rem;
      border: none;
      background: none;
      text-align: left;
      cursor: pointer;
      transition: background 0.2s;
    }

    .dropdown-item:hover {
      background: rgba(201, 168, 76, 0.1);
    }

    /* ── Modale d'aide ── */
    .help-body {
      text-align: center;
      padding: 0.5rem 0.5rem 1rem;
    }

    .help-icon {
      font-size: 2rem;
      color: #C9A84C;
      margin-bottom: 0.75rem;
    }

    .help-title {
      font-family: 'Cormorant Garamond', serif;
      font-size: 1.7rem;
      font-weight: 600;
      color: #F0E8D0;
      margin: 0 0 1rem;
    }

    .help-intro {
      color: #F0E8D0;
      opacity: 0.75;
      font-size: 0.92rem;
      line-height: 1.7;
      max-width: 500px;
      margin: 0 auto;
    }

    .help-intro strong {
      color: #C9A84C;
      opacity: 1;
    }

    .help-steps {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      text-align: left;
      max-width: 500px;
      margin: 1.75rem auto 0;
    }

    .help-step {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
    }

    .help-step-num {
      flex-shrink: 0;
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: rgba(201, 168, 76, 0.12);
      border: 1px solid rgba(201, 168, 76, 0.4);
      color: #C9A84C;
      font-size: 0.75rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
    }

    .help-step strong {
      display: block;
      font-size: 0.88rem;
      color: #F0E8D0;
      margin-bottom: 0.2rem;
    }

    .help-step p {
      font-size: 0.82rem;
      color: #F0E8D0;
      opacity: 0.6;
      line-height: 1.55;
      margin: 0;
    }

    .help-step em {
      color: #C9A84C;
      font-style: normal;
      font-weight: 500;
    }

    .help-footer {
      margin-top: 2rem;
    }

    .btn-help-start {
      display: inline-block;
      padding: 0.65rem 1.75rem;
      background: rgba(201, 168, 76, 0.12);
      border: 1px solid rgba(201, 168, 76, 0.5);
      border-radius: 8px;
      color: #C9A84C;
      font-size: 0.88rem;
      font-weight: 600;
      text-decoration: none;
      transition: background 0.2s;
    }

    .btn-help-start:hover {
      background: rgba(201, 168, 76, 0.22);
    }
  `]
})
export class NavbarComponent implements OnInit {
  currentUser$: Observable<User | null>;
  dropdownOpen = false;
  showHelp = false;

  constructor(private authService: AuthService) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {}

  getInitials(user: User): string {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  getPlanClass(plan: Plan): string {
    switch (plan) {
      case 'FREE': return 'plan-free';
      case 'PREMIUM': return 'plan-premium';
      case 'PREMIUM_PLUS': return 'plan-premium-plus';
    }
  }

  getPlanLabel(plan: Plan): string {
    switch (plan) {
      case 'FREE': return 'Free';
      case 'PREMIUM': return 'Premium';
      case 'PREMIUM_PLUS': return 'Premium+';
    }
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  logout(): void {
    this.authService.logout();
  }
}
