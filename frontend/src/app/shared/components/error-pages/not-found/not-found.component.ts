import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="error-page">
      <div class="error-brand">Joseph &middot; Yusuf</div>
      <div class="error-icon" aria-hidden="true">&#x1F50D;</div>
      <h1 class="error-title">Page introuvable</h1>
      <p class="error-subtitle">
        La page que vous cherchez n'existe pas ou a &eacute;t&eacute; d&eacute;plac&eacute;e.
      </p>
      <div class="error-actions">
        <a routerLink="/dashboard" class="btn-primary">Retour au Dashboard</a>
      </div>
      <p class="error-contact">
        <a routerLink="/support">Contacter le support</a>
      </p>
    </div>
  `,
  styleUrls: ['../error-page.scss']
})
export class NotFoundComponent {}
