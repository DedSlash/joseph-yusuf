import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { User, Plan } from '../../../shared/models/user.model';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationBellComponent],
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
        <span class="plan-badge" [ngClass]="getPlanClass(user.plan)">
          {{ getPlanLabel(user.plan) }}
        </span>
        <div class="avatar-container" (click)="toggleDropdown()">
          <div class="avatar">{{ getInitials(user) }}</div>
          <div class="dropdown" *ngIf="dropdownOpen">
            <a class="dropdown-item" routerLink="/account">Mon compte</a>
            <button class="dropdown-item" (click)="logout()">Deconnexion</button>
          </div>
        </div>
      </div>
    </nav>
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
  `]
})
export class NavbarComponent implements OnInit {
  currentUser$: Observable<User | null>;
  dropdownOpen = false;

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
