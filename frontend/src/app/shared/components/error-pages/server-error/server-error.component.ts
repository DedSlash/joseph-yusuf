import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-server-error',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="error-page">
      <div class="error-brand">Joseph &middot; Yusuf</div>
      <div class="error-card card">
        <div class="error-icon" aria-hidden="true">&#x2699;&#xFE0F;</div>
        <h1 class="error-title">Quelque chose s'est mal pass&eacute;</h1>
        <p class="error-subtitle">
          Nos &eacute;quipes ont &eacute;t&eacute; notifi&eacute;es. Veuillez r&eacute;essayer dans quelques instants.
        </p>
        <div class="error-actions">
          <button type="button" class="btn-primary" (click)="retry()">R&eacute;essayer</button>
          <a routerLink="/dashboard" class="btn-secondary">Retour au Dashboard</a>
        </div>
        <p class="retry-counter" *ngIf="countdown > 0">
          Nouvelle tentative dans {{ countdown }}s...
        </p>
        <p class="error-contact">
          <a [routerLink]="['/support']" [queryParams]="{ subject: 'Erreur 500 rencontr&eacute;e' }">
            Signaler le probl&egrave;me
          </a>
        </p>
      </div>
    </div>
  `,
  styleUrls: ['../error-page.scss']
})
export class ServerErrorComponent implements OnInit, OnDestroy {
  countdown = 30;
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    this.timerId = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        this.retry();
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  retry(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
    this.router.navigate(['/dashboard']);
  }
}
