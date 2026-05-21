import { Component, ElementRef, HostListener, OnInit } from '@angular/core';
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
        <button class="nav-burger" (click)="toggleDrawer($event)" [class.open]="drawerOpen" aria-label="Menu">
          <span></span><span></span><span></span>
        </button>
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
            <a class="dropdown-item" routerLink="/account" (click)="dropdownOpen = false">Mon compte</a>
            <a class="dropdown-item" routerLink="/subscription" (click)="dropdownOpen = false">Mon abonnement</a>
            <a class="dropdown-item" routerLink="/support" (click)="dropdownOpen = false">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14" style="vertical-align: -2px; margin-right: 4px; opacity: 0.7">
                <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
              </svg>Support
            </a>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item" (click)="logout()">Déconnexion</button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Drawer mobile -->
    <div class="nav-drawer-overlay" *ngIf="drawerOpen" (click)="drawerOpen = false"></div>
    <aside class="nav-drawer" [class.open]="drawerOpen">
      <a routerLink="/dashboard" routerLinkActive="active" class="drawer-link" (click)="drawerOpen = false">Dashboard</a>
      <a routerLink="/incomes" routerLinkActive="active" class="drawer-link" (click)="drawerOpen = false">Mes Revenus</a>
      <a routerLink="/account" class="drawer-link" (click)="drawerOpen = false">Mon compte</a>
      <a routerLink="/subscription" class="drawer-link" (click)="drawerOpen = false">Mon abonnement</a>
      <a routerLink="/support" class="drawer-link" (click)="drawerOpen = false">Support</a>
      <div class="drawer-divider"></div>
      <button class="drawer-link drawer-logout" (click)="drawerOpen = false; logout()">Déconnexion</button>
    </aside>

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
      height: 60px;
      background: rgba(8, 8, 15, 0.7);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 28px;
      z-index: 1000;
    }

    .navbar-left {
      display: flex;
      align-items: center;
      gap: 2rem;
    }

    .logo {
      font-family: var(--font-serif);
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--text-0);
      text-decoration: none;
      letter-spacing: -0.01em;
      display: flex;
      align-items: center;
      gap: 10px;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .logo:hover { color: var(--gold-light); }

    .nav-links {
      display: flex;
      gap: 4px;
    }

    .nav-link {
      position: relative;
      padding: 8px 14px;
      border-radius: 8px;
      color: var(--text-2);
      text-decoration: none;
      font-size: 13.5px;
      font-weight: 500;
      transition: color 0.15s, background 0.15s;
    }

    .nav-link:hover {
      color: var(--text-0);
      background: rgba(255, 255, 255, 0.04);
    }

    .nav-link.active {
      color: var(--gold-light);
    }

    .nav-link.active::after {
      content: "";
      position: absolute;
      left: 14px; right: 14px; bottom: 2px;
      height: 2px;
      background: linear-gradient(90deg, var(--gold), var(--gold-light));
      border-radius: 2px;
    }

    .navbar-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .plan-badge {
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .plan-free {
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-2);
      border: 1px solid rgba(255, 255, 255, 0.06);
    }

    .plan-premium {
      background: var(--gold-tint);
      color: var(--gold-light);
      border: 1px solid rgba(201, 168, 76, 0.32);
    }

    .plan-premium-plus {
      background: linear-gradient(135deg, rgba(232, 200, 118, 0.2), rgba(157, 130, 53, 0.1));
      color: var(--gold-light);
      border: 1px solid var(--gold);
    }

    .btn-help {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.06);
      border-radius: 8px;
      color: var(--text-1);
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
      white-space: nowrap;
    }

    .btn-help:hover {
      background: rgba(255, 255, 255, 0.08);
      color: var(--text-0);
    }

    .btn-upgrade-nav {
      padding: 7px 14px;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      border-radius: 8px;
      color: #1b1500;
      font-size: 12px;
      font-weight: 700;
      text-decoration: none;
      transition: box-shadow 0.2s;
      white-space: nowrap;
      box-shadow: 0 4px 12px -4px var(--gold-glow);
    }

    .btn-upgrade-nav:hover { box-shadow: 0 8px 20px -4px var(--gold-glow); }

    .avatar-container {
      position: relative;
      cursor: pointer;
    }

    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--gold-light), var(--gold-deep));
      color: #1b1500;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      letter-spacing: 0.02em;
    }

    .dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: var(--night-2);
      border: 1px solid var(--line-strong);
      border-radius: 12px;
      min-width: 180px;
      padding: 6px;
      box-shadow: 0 20px 60px -15px rgba(0, 0, 0, 0.6);
    }

    .dropdown-item {
      display: block;
      width: 100%;
      padding: 10px 14px;
      color: var(--text-1);
      text-decoration: none;
      font-size: 13.5px;
      border: none;
      background: none;
      text-align: left;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
      border-radius: 8px;
    }

    .dropdown-item:hover {
      background: rgba(255, 255, 255, 0.04);
      color: var(--text-0);
    }

    .dropdown-divider {
      height: 1px;
      background: var(--line-soft);
      margin: 4px 8px;
    }

    /* ── Modale d'aide ── */
    .help-body {
      text-align: center;
      padding: 0.5rem 0.5rem 1rem;
    }

    .help-icon {
      font-size: 2rem;
      color: var(--gold);
      margin-bottom: 0.75rem;
    }

    .help-title {
      font-family: var(--font-serif);
      font-size: 1.7rem;
      font-weight: 600;
      color: var(--text-0);
      margin: 0 0 1rem;
    }

    .help-intro {
      color: var(--text-1);
      font-size: 0.92rem;
      line-height: 1.7;
      max-width: 500px;
      margin: 0 auto;
    }

    .help-intro strong {
      color: var(--gold-light);
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
      padding: 12px 14px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--line-soft);
    }

    .help-step-num {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--gold-tint);
      border: 1px solid var(--line-strong);
      color: var(--gold-light);
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
      color: var(--text-0);
      margin-bottom: 0.2rem;
    }

    .help-step p {
      font-size: 0.82rem;
      color: var(--text-2);
      line-height: 1.55;
      margin: 0;
    }

    .help-step em {
      color: var(--gold-light);
      font-style: normal;
      font-weight: 500;
    }

    .help-footer {
      margin-top: 2rem;
    }

    .btn-help-start {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 11px 24px;
      background: linear-gradient(180deg, var(--gold-light), var(--gold));
      border-radius: 10px;
      color: #1b1500;
      font-size: 0.88rem;
      font-weight: 600;
      text-decoration: none;
      transition: box-shadow 0.2s;
      box-shadow: 0 8px 24px -8px var(--gold-glow);
    }

    .btn-help-start:hover {
      box-shadow: 0 12px 32px -8px var(--gold-glow);
    }

    /* ── Burger button ── */
    .nav-burger {
      display: none;
      flex-direction: column;
      justify-content: center;
      gap: 5px;
      width: 32px;
      height: 32px;
      padding: 6px;
      background: transparent;
      border: none;
      cursor: pointer;
      margin-right: 4px;
    }
    .nav-burger span {
      display: block;
      width: 20px;
      height: 2px;
      background: var(--text-0);
      border-radius: 2px;
      transition: transform 0.25s, opacity 0.25s;
    }
    .nav-burger.open span:nth-child(1) { transform: translateY(7px) rotate(45deg); }
    .nav-burger.open span:nth-child(2) { opacity: 0; }
    .nav-burger.open span:nth-child(3) { transform: translateY(-7px) rotate(-45deg); }

    /* ── Drawer ── */
    .nav-drawer-overlay {
      position: fixed; inset: 0;
      background: rgba(0, 0, 0, 0.55);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      z-index: 998;
      animation: fade-in 0.2s ease-out;
    }
    .nav-drawer {
      position: fixed;
      top: 0; left: 0;
      width: 270px;
      max-width: 80vw;
      height: 100vh;
      background: rgba(13, 14, 28, 0.98);
      backdrop-filter: blur(20px) saturate(160%);
      -webkit-backdrop-filter: blur(20px) saturate(160%);
      border-right: 1px solid rgba(201, 168, 76, 0.18);
      padding: 76px 1rem 1.5rem;
      z-index: 999;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      transform: translateX(-100%);
      transition: transform 0.3s cubic-bezier(0.2, 0.7, 0.2, 1);
    }
    .nav-drawer.open { transform: translateX(0); }
    .drawer-link {
      padding: 0.85rem 1rem;
      color: var(--text-1);
      text-decoration: none;
      font-size: 0.95rem;
      border-radius: 8px;
      transition: background 0.15s, color 0.15s;
      text-align: left;
      background: none;
      border: none;
      cursor: pointer;
      font-family: inherit;
    }
    .drawer-link:hover, .drawer-link.active {
      background: rgba(201,168,76,0.1);
      color: var(--gold-light);
    }
    .drawer-divider { height: 1px; background: var(--line-soft); margin: 0.5rem 0; }
    .drawer-logout { color: #ff7a6c; }
    .drawer-logout:hover { background: rgba(255, 122, 108, 0.08); color: #ff7a6c; }

    /* Tablet : 768px – 1023px */
    @media (min-width: 768px) and (max-width: 1023px) {
      .navbar { padding: 0 20px; }
      .navbar-right { gap: 8px; }
      .btn-help span { display: none; }
    }

    /* Mobile : ≤ 767px */
    @media (max-width: 767px) {
      .navbar { padding: 0 12px; height: 56px; }
      .nav-burger { display: flex; }
      .nav-links { display: none; }
      .btn-help span { display: none; }
      .btn-upgrade-nav { padding: 6px 10px; font-size: 11px; }
      .plan-badge { padding: 3px 8px; font-size: 10px; }
      .logo { font-size: 1rem; }
      .navbar-right { gap: 6px; }
    }
  `]
})
export class NavbarComponent implements OnInit {
  currentUser$: Observable<User | null>;
  dropdownOpen = false;
  drawerOpen = false;
  showHelp = false;

  toggleDrawer(event: Event): void {
    event.stopPropagation();
    this.drawerOpen = !this.drawerOpen;
  }

  constructor(private readonly authService: AuthService, private readonly elRef: ElementRef) {
    this.currentUser$ = this.authService.currentUser$;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.dropdownOpen) {
      const avatarContainer = this.elRef.nativeElement.querySelector('.avatar-container');
      if (avatarContainer && !avatarContainer.contains(event.target)) {
        this.dropdownOpen = false;
      }
    }
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
