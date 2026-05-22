import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-offline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="error-page">
      <div class="error-brand">Joseph &middot; Yusuf</div>
      <div class="error-card card">
        <div class="error-icon" aria-hidden="true">&#x1F4E1;</div>
        <h1 class="error-title">Pas de connexion internet</h1>
        <p class="error-subtitle">
          V&eacute;rifiez votre connexion et r&eacute;essayez.
        </p>
        <div class="error-actions">
          <button type="button" class="btn-primary" (click)="retry()">R&eacute;essayer</button>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['../error-page.scss']
})
export class OfflineComponent implements OnInit, OnDestroy {
  private readonly onlineListener = () => this.retry();

  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    window.addEventListener('online', this.onlineListener);
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.onlineListener);
  }

  retry(): void {
    if (navigator.onLine) {
      this.router.navigate(['/dashboard']);
    }
  }
}
