import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { AnalyticsService } from './core/services/analytics.service';
import { SupportButtonComponent } from './features/support/support-button.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, SupportButtonComponent],
  template: `
    <app-navbar *ngIf="!isAuthPage()"></app-navbar>
    <router-outlet></router-outlet>
    <app-support-button *ngIf="!isAuthPage() && authService.isLoggedIn()"></app-support-button>
  `
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly visibilityHandler = () => {
    if (document.visibilityState === 'visible' && this.authService.isLoggedIn()) {
      this.authService.refreshSession().subscribe();
    }
  };

  constructor(private readonly router: Router, protected readonly authService: AuthService, private readonly analytics: AnalyticsService) {}

  ngOnInit(): void {
    this.analytics.init();
    document.addEventListener('visibilitychange', this.visibilityHandler);
  }

  ngOnDestroy(): void {
    document.removeEventListener('visibilitychange', this.visibilityHandler);
  }

  isAuthPage(): boolean {
    const url = this.router.url.split('?')[0];
    return url === '/' || url === '/login' || url === '/register'
      || url === '/forgot-password' || url === '/reset-password';
  }
}
